package com.hazemjday.phogene

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.*
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.hazemjday.phogene.databinding.ActivityGenerateBinding
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class GenerateActivity : AppCompatActivity() {
    lateinit var binding: ActivityGenerateBinding
    private lateinit var requestQueue: RequestQueue
    private lateinit var auth: FirebaseAuth
    private lateinit var imagesRef: DatabaseReference
    private lateinit var progressDialog: ProgressDialog
    private val url = "http://192.168.43.129/sdapi/v1/txt2img"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGenerateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        imagesRef = FirebaseDatabase.getInstance().reference.child("images").child(auth.uid!!)


        requestQueue = Volley.newRequestQueue(this)

        binding.stepsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.stepsText.text = "Steps : $progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        binding.giudanceSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                binding.guidanceText.text = "Guidance : $progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        binding.submit.setOnClickListener {
            val style = binding.styleSpinner.selectedItem as String
            var prompt = binding.promptEditText.text.toString()
            prompt = if(style=="Realistic") "$prompt,(( Realistic ))" else if(style=="3D") "$prompt,(( Pixar style ))" else "$prompt,(( Anime ))"
            progressDialog = ProgressDialog.show(this, "", "Generating image..", true)
            generate(
                prompt,
                binding.negativeEditText.text.toString(),
                512,
                512,
                binding.stepsSeekBar.progress,
                binding.giudanceSeekBar.progress,
                binding.samplerSpinner.selectedItem as String,
            )
        }

        binding.save.setOnClickListener{
            saveImageToGallery()
        }
        binding.share.setOnClickListener{
            shareImage()

        }


    }


    private fun generate(
        prompt: String,
        negative: String,
        width: Int,
        height: Int,
        steps: Int,
        guidance: Int/*, model : String,*/,
        sampler: String
    ) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("prompt", prompt)
            jsonObject.put("steps", steps)
            jsonObject.put("cfg_scale", guidance)
            jsonObject.put("width", width)
            jsonObject.put("height", height)
            jsonObject.put("negative_prompt", negative)
            jsonObject.put("sampler_name", sampler)

        } catch (e: JSONException) {
            e.printStackTrace()

        }


        val stringRequest: StringRequest = @RequiresApi(Build.VERSION_CODES.O)
        object : StringRequest(
            Method.POST, url,
            Response.Listener { response: String? ->
                try {
                    val jsonObject1 = JSONObject(response)
                    val images = jsonObject1.getJSONArray("images")
                    val image = images.getString(0)
                    val decodedString =
                        Base64.decode(image, Base64.DEFAULT)
                    val bitmap =
                        BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                    binding.image.setImageBitmap(bitmap)
                    imagesRef.child(System.currentTimeMillis().toString())
                        .setValue(
                            Image(
                                prompt,
                                negative,
                                width,
                                height,
                                steps,
                                guidance,
                                sampler,
                                images.getString(0)
                            )
                        )

                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(this, "err", Toast.LENGTH_SHORT).show()
                }
                binding.submit.isEnabled = true
                progressDialog.dismiss()
            },

            Response.ErrorListener { error: VolleyError? ->
                binding.submit.isEnabled = true
                progressDialog.dismiss()
                Toast.makeText(this, error!!.message, Toast.LENGTH_SHORT).show()

            }) {
            @Throws(AuthFailureError::class)
            override fun getBody(): ByteArray {
                return jsonObject.toString().toByteArray()
            }

            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers: MutableMap<String, String> = HashMap()
                headers["Content-Type"] = "application/json"
                return headers
            }
        }
        stringRequest.retryPolicy = DefaultRetryPolicy(
            500000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        requestQueue.add<String>(stringRequest)
    }



    private fun saveImageToGallery() {
        val bitmap = Bitmap.createBitmap(binding.image.width, binding.image.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        binding.image.draw(canvas)
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        val filename = "${System.currentTimeMillis()}.png"
        MediaStore.Images.Media.insertImage(this.contentResolver, bitmap, filename, null)
        Toast.makeText(this,"Saved $filename",Toast.LENGTH_SHORT).show()
    }

    private fun shareImage() {
        val bitmap = Bitmap.createBitmap(binding.image.width, binding.image.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        binding.image.draw(canvas)

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "image/*"

        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        val filename = "${System.currentTimeMillis()}.png"
        val path = MediaStore.Images.Media.insertImage(this.contentResolver, bitmap, filename, null)

        val uri = Uri.parse(path)
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        this.startActivity(Intent.createChooser(shareIntent, "Share image"))
    }


}