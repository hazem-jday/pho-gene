package com.hazemjday.phogene.ui.settings

import android.app.ProgressDialog
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.hazemjday.phogene.R
import com.hazemjday.phogene.databinding.FragmentSettingsBinding
import org.json.JSONException
import org.json.JSONObject

class SettingsFragment : Fragment() {
    private lateinit var requestQueue: RequestQueue
    private var _binding: FragmentSettingsBinding? = null
    private var url = "http://192.168.43.129/sdapi/v1/options"
    private val binding get() = _binding!!
    private lateinit var progressDialog: ProgressDialog
    private var start = true
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        start = true
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        requestQueue = Volley.newRequestQueue(context)
        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> binding.radioButtonDark.isChecked = true
            Configuration.UI_MODE_NIGHT_NO -> binding.radioButtonLight.isChecked = true
        }

        getModel()


        _binding!!.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioButtonLight -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    requireActivity().recreate()

                }
                R.id.radioButtonDark -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    requireActivity().recreate()
                }
            }
        }



        _binding!!.spinnerModels.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                if(! start){
                    val selectedItem = parent?.getItemAtPosition(position).toString()
                    progressDialog = ProgressDialog.show(context, "", "Loading models..", true)
                    switchModel(selectedItem)
                }
                start = false


            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }



    private fun switchModel(
        model: String,
    ) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("sd_model_checkpoint", model)

        } catch (e: JSONException) {
            e.printStackTrace()

        }

        val stringRequest: StringRequest = @RequiresApi(Build.VERSION_CODES.O)
        object : StringRequest(
            Method.POST, url,
            Response.Listener { response: String? ->

                progressDialog.dismiss()
            },

            Response.ErrorListener { error: VolleyError? ->
                progressDialog.dismiss()

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
        requestQueue.add(stringRequest)
    }

    fun getModel(){
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url,null,
            { response ->
                val model = response.getString("sd_model_checkpoint")
                if(model.contains("midjourney")){
                    binding.spinnerModels.setSelection(0)
                }
                else{
                    binding.spinnerModels.setSelection(1)
                }


            },
            { error ->
                Log.e("Err", "Error : $error")
            })
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            500000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        requestQueue.add(jsonObjectRequest)
    }
}