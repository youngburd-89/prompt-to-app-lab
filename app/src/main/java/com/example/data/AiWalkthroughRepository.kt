package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.model.AiDetectedItemDraft
import com.example.model.AiWalkthroughAnalysis
import com.example.model.ItemCategory
import com.example.model.ItemRisk
import com.example.model.RiskLevel
import com.example.model.TruckSize
import com.example.model.ValuationTier
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import kotlin.math.max

private const val TAG = "AiWalkthroughRepo"
private const val AI_TIMEOUT_MS = 60_000L
private const val MAX_AI_FRAMES = 5
private const val AI_MAX_DIM = 768
private const val THUMB_MAX_DIM = 240

class AiWalkthroughRepository(private val context: Context) {

    private val walkthroughSchema = Schema.obj(
        properties = mapOf(
            "items" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(description = "Specific furniture, appliance, or box group name"),
                        "room" to Schema.string(description = "Room where the item is located"),
                        "category" to Schema.enumeration(
                            values = listOf("STANDARD", "FRAGILE", "HEAVY", "HIGH_VALUE"),
                            description = "Primary handling classification"
                        ),
                        "estimatedValueUsd" to Schema.integer(description = "Estimated replacement value in USD"),
                        "riskLevel" to Schema.enumeration(
                            values = listOf("LOW", "MEDIUM", "HIGH"),
                            description = "Damage risk during packing and transit"
                        ),
                        "handlingNote" to Schema.string(description = "Concise crew handling or crating instruction")
                    )
                ),
                description = "List of identified items and risk flags from the walkthrough"
            ),
            "recommendedTruckSize" to Schema.enumeration(
                values = listOf("CARGO_VAN", "TRUCK_15FT", "TRUCK_20FT", "TRUCK_26FT"),
                description = "Recommended just-in-time rental truck size"
            ),
            "estimatedCrewSize" to Schema.integer(description = "Recommended crew size (minimum 2)"),
            "estimatedHours" to Schema.double(description = "Estimated hours for packing, move, and cleanup"),
            "recommendedBoxes" to Schema.integer(description = "Estimated moving boxes needed"),
            "recommendedWrapRolls" to Schema.integer(description = "Estimated stretch wrap and bubble rolls needed"),
            "estimatedTotalItemValueUsd" to Schema.integer(description = "Total estimated value of visible contents in USD"),
            "overallRiskLevel" to Schema.enumeration(
                values = listOf("LOW", "MODERATE", "ELEVATED"),
                description = "Overall move complexity and fragility risk"
            ),
            "recommendedValuationTier" to Schema.enumeration(
                values = listOf("RELEASED_VALUE", "FULL_VALUE"),
                description = "Recommended protection tier based on fragile and high-value items"
            ),
            "summaryNote" to Schema.string(description = "Short actionable pre-move summary for the customer and crew")
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = walkthroughSchema
            }
        )
    }

    suspend fun analyzeWalkthrough(
        photoUris: List<Uri>,
        videoUri: Uri?,
        squareFootageHint: Int,
        roomCountHint: Int
    ): AiWalkthroughAnalysis {
        val frames = withContext(Dispatchers.IO) {
            extractMediaBitmaps(photoUris, videoUri)
        }
        require(frames.isNotEmpty()) {
            "Select at least one room photo or walkthrough video to run the AI estimator."
        }

        val thumbnailsBase64 = withContext(Dispatchers.IO) {
            frames.take(3).mapNotNull { encodeThumbnailBase64(it) }
        }

        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                model.generateContent(
                    content {
                        frames.forEach { bitmap ->
                            image(bitmap)
                        }
                        text(
                            """
                            You are the AI Pre-Move Estimator and Condition Auditor for Back Up ("Call for Reinforcements!"),
                            an on-demand moving, packing, and move-out cleaning company with 2-person+ crews and just-in-time rental trucks.
                            Inspect these room walkthrough images/frames (home hint: ~${squareFootageHint} sq ft, ~${roomCountHint} rooms).
                            Identify visible furniture, electronics, fragile items (glass, mirrors, art, lamps), heavy/bulky items (sectionals, pianos, safes, appliances), and high-value items.
                            Flag each item's room, category (STANDARD, FRAGILE, HEAVY, HIGH_VALUE), estimated USD value, risk level (LOW, MEDIUM, HIGH), and crew handling instruction.
                            Recommend the right just-in-time truck size, crew size (>=2), estimated hours, box & wrap counts, overall risk, and valuation tier (FULL_VALUE if high-value or multiple fragile items are present).
                            """.trimIndent()
                        )
                    }
                )
            }
            val rawJson = response.text
                ?: throw IllegalStateException("AI Estimator returned an empty response.")
            parseAnalysisJson(rawJson, thumbnailsBase64)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI walkthrough analysis timed out", e)
            throw IllegalStateException("AI walkthrough timed out after 60 seconds. Please retry with fewer photos.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI walkthrough analysis failed", e)
            throw e
        }
    }

    private fun parseAnalysisJson(
        jsonString: String,
        thumbnailsBase64: List<String>
    ): AiWalkthroughAnalysis {
        val root = JSONObject(jsonString)
        val itemsJson = root.optJSONArray("items")
        val parsedItems = buildList {
            if (itemsJson != null) {
                for (i in 0 until itemsJson.length()) {
                    val obj = itemsJson.optJSONObject(i) ?: continue
                    val thumb = thumbnailsBase64.getOrNull(i % max(1, thumbnailsBase64.size))
                        ?.takeIf { i < 3 }
                    add(
                        AiDetectedItemDraft(
                            name = obj.optString("name", "Household Item").take(115),
                            room = obj.optString("room", "Main Area").take(75),
                            category = ItemCategory.fromWire(obj.optString("category")),
                            estimatedValueUsd = obj.optInt("estimatedValueUsd", 250).coerceIn(0, 1_000_000),
                            riskLevel = ItemRisk.fromWire(obj.optString("riskLevel")),
                            handlingNote = obj.optString("handlingNote", "Standard pad & wrap").take(450),
                            thumbnailBase64 = thumb
                        )
                    )
                }
            }
        }

        return AiWalkthroughAnalysis(
            items = parsedItems,
            recommendedTruckSize = TruckSize.fromWire(root.optString("recommendedTruckSize")),
            estimatedCrewSize = root.optInt("estimatedCrewSize", 2).coerceIn(2, 8),
            estimatedHours = root.optDouble("estimatedHours", 4.5).coerceIn(1.0, 60.0),
            recommendedBoxes = root.optInt("recommendedBoxes", 24).coerceIn(0, 400),
            recommendedWrapRolls = root.optInt("recommendedWrapRolls", 3).coerceIn(1, 50),
            estimatedTotalItemValueUsd = root.optInt("estimatedTotalItemValueUsd", 5000).coerceIn(0, 1_000_000),
            overallRiskLevel = RiskLevel.fromWire(root.optString("overallRiskLevel")),
            recommendedValuationTier = ValuationTier.fromWire(root.optString("recommendedValuationTier")),
            summaryNote = root.optString("summaryNote", "AI pre-move condition record generated.").take(1400),
            capturedThumbnailsBase64 = thumbnailsBase64
        )
    }

    private fun extractMediaBitmaps(photoUris: List<Uri>, videoUri: Uri?): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        for (uri in photoUris.take(MAX_AI_FRAMES)) {
            decodeScaledBitmap(uri)?.let { bitmaps.add(it) }
        }

        if (videoUri != null && bitmaps.size < MAX_AI_FRAMES) {
            val remainingSlots = MAX_AI_FRAMES - bitmaps.size
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)
                val durationMs = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0L
                if (durationMs > 0L) {
                    for (i in 0 until remainingSlots) {
                        val fraction = (i + 0.5) / remainingSlots.toDouble()
                        val timeUs = (durationMs * fraction * 1000.0).toLong()
                        val frame = retriever.getFrameAtTime(
                            timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )
                        if (frame != null) {
                            bitmaps.add(scaleBitmap(frame, AI_MAX_DIM))
                        }
                    }
                } else {
                    retriever.getFrameAtTime(0)?.let { bitmaps.add(scaleBitmap(it, AI_MAX_DIM)) }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Unable to extract video walkthrough frames", e)
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        }
        return bitmaps
    }

    private fun decodeScaledBitmap(uri: Uri): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val maxSide = max(bounds.outWidth, bounds.outHeight)
            var sampleSize = 1
            while (maxSide / sampleSize > AI_MAX_DIM * 2) {
                sampleSize *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
            } ?: return null
            scaleBitmap(decoded, AI_MAX_DIM)
        } catch (e: Exception) {
            Log.w(TAG, "Failed decoding image URI: $uri", e)
            null
        }
    }

    fun decodeThumbnailBase64FromUri(uri: Uri): String? {
        val bitmap = decodeScaledBitmap(uri) ?: return null
        return encodeThumbnailBase64(bitmap)
    }

    private fun scaleBitmap(source: Bitmap, maxDim: Int): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= maxDim && h <= maxDim) return source
        val scale = maxDim.toFloat() / max(w, h).toFloat()
        val targetW = (w * scale).toInt().coerceAtLeast(1)
        val targetH = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    private fun encodeThumbnailBase64(source: Bitmap): String? {
        return try {
            val thumb = scaleBitmap(source, THUMB_MAX_DIM)
            val baos = ByteArrayOutputStream()
            thumb.compress(Bitmap.CompressFormat.JPEG, 65, baos)
            val bytes = baos.toByteArray()
            if (bytes.size > 80_000) null else Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }
}
