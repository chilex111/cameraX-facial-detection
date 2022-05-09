package com.androchef.cameraxfacedetection.utils

import android.graphics.Bitmap
import android.util.Log
import com.androchef.cameraxfacedetection.CompareImageActivity
import com.androchef.cameraxfacedetection.models.FaceNetModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt

class RunModel ( private var model: FaceNetModel){
    private var subject = FloatArray( model.embeddingDim )
    private var subject2 = FloatArray( model.embeddingDim )

    // Store the face embeddings in a ( String , FloatArray ) ArrayList.
    // Where String -> name of the person and FloatArray -> Embedding of the face.
    var faceList = ArrayList<Pair<String,FloatArray>>()
    //private val nameScoreHashmap = HashMap<String,ArrayList<Float>>()

    // Use any one of the two metrics, "cosine" or "l2"
    private val metricToBeUsed = "l2"
    //private val metricToBeUsed = "cosine"

    suspend fun runModel(bitmap1: Bitmap, bitmap2: Bitmap){
        var  similarFloat=0.0f
        var similarity:String? =null
        withContext( Dispatchers.Default ) {
                try {
                    Log.e(TAG, "BEFORE:: $subject /// $subject2")
                    val bmp1: Bitmap = bitmap1.copy(Bitmap.Config.ARGB_8888, true)
                    val bmp2: Bitmap = bitmap2.copy(Bitmap.Config.ARGB_8888, true)


                    subject = model.getFaceEmbedding( bmp1 )
                    subject2 = model.getFaceEmbedding( bmp2 )

                    Log.e(TAG, "AFTER:: $subject /// $subject2")

                 similarFloat = if ( metricToBeUsed == "cosine" ) {
                        cosineSimilarity( subject , subject2 )
                    } else {
                        L2Norm( subject , subject2)
                    }

                 /*   similarity = if ( similarFloat > model.model.cosineThreshold ) {
                        null
                    } else{
                        similarFloat.toString()
                    }*/
                     similarity = if ( similarFloat > model.model.l2Threshold ) {
                        null
                    } else{
                        similarFloat.toString()
                    }
                  Log.e( TAG ,"Average score for each user: $similarFloat")
                    Log.e( TAG ,"Average score for each user $similarFloat")

                }
                catch ( e : Exception ) {
                    Log.e( "Model" , "Exception in FrameAnalyser : ${e.message}" )

                }

            withContext( Dispatchers.Main ) {
                CompareImageActivity.setMessage( similarity.toString() )
                similarFloat = 0.0f
            }
        }
    }

    /*
    suspend fun runModel(faces : List<Face>, cameraFrameBitmap : Bitmap){
        withContext( Dispatchers.Default ) {
            //  val predictions = ArrayList<Prediction>()
            for (face in faces) {
                try {
                    // Crop the frame using face.boundingBox.
                    // Convert the cropped Bitmap to a ByteBuffer.
                    // Finally, feed the ByteBuffer to the FaceNet model.
                    val croppedBitmap = BitmapUtils.cropRectFromBitmap( cameraFrameBitmap , face.boundingBox )
                    subject = model.getFaceEmbedding( croppedBitmap )

                    // Perform face mask detection on the cropped frame Bitmap.
                    //var maskLabel = ""
                    //  if ( isMaskDetectionOn ) {
                    //  maskLabel = maskDetectionModel.detectMask( croppedBitmap )
                    // }

                    // Continue with the recognition if the user is not wearing a face mask
                    // if (maskLabel == maskDetectionModel.NO_MASK) {
                    // Perform clustering ( grouping )
                    // Store the clusters in a HashMap. Here, the key would represent the 'name'
                    // of that cluster and ArrayList<Float> would represent the collection of all
                    // L2 norms/ cosine distances.
                    for ( i in 0 until faceList.size ) {
                        // If this cluster ( i.e an ArrayList with a specific key ) does not exist,
                        // initialize a new one.
                        if ( nameScoreHashmap[ faceList[ i ].first ] == null ) {
                            // Compute the L2 norm and then append it to the ArrayList.
                            val p = ArrayList<Float>()
                            if ( metricToBeUsed == "cosine" ) {
                                p.add( cosineSimilarity( subject , faceList[ i ].second ) )
                            }
                            else {
                                p.add( L2Norm( subject , faceList[ i ].second ) )
                            }
                            nameScoreHashmap[ faceList[ i ].first ] = p
                        }
                        // If this cluster exists, append the L2 norm/cosine score to it.
                        else {
                            if ( metricToBeUsed == "cosine" ) {
                                nameScoreHashmap[ faceList[ i ].first ]?.add( cosineSimilarity( subject , faceList[ i ].second ) )
                            }
                            else {
                                nameScoreHashmap[ faceList[ i ].first ]?.add( L2Norm( subject , faceList[ i ].second ) )
                            }
                        }
                    }

                    // Compute the average of all scores norms for each cluster.
                    val avgScores = nameScoreHashmap.values.map{ scores -> scores.toFloatArray().average() }
                    Log.e( TAG ,"Average score for each user: $nameScoreHashmap")
                    Log.e( TAG ,"Average score for each user $avgScores")

                    *//*    val names = nameScoreHashmap.keys.toTypedArray()
                      nameScoreHashmap.clear()

                      // Calculate the minimum L2 distance from the stored average L2 norms.
                      val bestScoreUserName: String = if ( metricToBeUsed == "cosine" ) {
                          // In case of cosine similarity, choose the highest value.
                          if ( avgScores.maxOrNull()!! > model.model.cosineThreshold ) {
                              names[ avgScores.indexOf( avgScores.maxOrNull()!! ) ]
                          }
                          else {
                              "Unknown"
                          }
                      } else {
                          Log.e("L2", avgScores.minOrNull().toString())
                          // In case of L2 norm, choose the lowest value.
                          if ( avgScores.minOrNull()!! > model.model.l2Threshold ) {
                              val range = (model.model.l2Threshold..10F)
                              if (avgScores.minOrNull()!! in range)
                                  names[ avgScores.indexOf( avgScores.minOrNull()!! ) ]
                              "unknown"
                          }
                          else {
                              names[ avgScores.indexOf( avgScores.minOrNull()!! ) ]
                          }
                      }
                       Logger.log( "Person identified as $bestScoreUserName" )
                         predictions.add(
                             Prediction(
                                 face.boundingBox,
                                 bestScoreUserName ,
                                 //maskLabel
                             )
                         )*//*
                }
                catch ( e : Exception ) {
                    // If any exception occurs with this box and continue with the next boxes.
                    Log.e( "Model" , "Exception in FrameAnalyser : ${e.message}" )
                    continue
                }
            }
            withContext( Dispatchers.Main ) {
                // Clear the BoundingBoxOverlay and set the new results ( boxes ) to be displayed.
              *//*  boundingBoxOverlay.faceBoundingBoxes = predictions
                boundingBoxOverlay.invalidate()
                CompareImageActivity.setMessage( aver )


                isProcessing = false*//*
            }
        }
    }*/
    // Compute the L2 norm of ( x2 - x1 )
    private fun L2Norm( x1 : FloatArray, x2 : FloatArray ) : Float {
        return sqrt( x1.mapIndexed{ i , xi -> (xi - x2[ i ]).pow( 2 ) }.sum() )
    }


    // Compute the cosine of the angle between x1 and x2.
    private fun cosineSimilarity( x1 : FloatArray , x2 : FloatArray ) : Float {
        val mag1 = sqrt( x1.map { it * it }.sum() )
        val mag2 = sqrt( x2.map { it * it }.sum() )
        val dot = x1.mapIndexed{ i , xi -> xi * x2[ i ] }.sum()
        return dot / (mag1 * mag2)
    }

    companion object{
        const val TAG="RUN_MODEL"
    }
}
