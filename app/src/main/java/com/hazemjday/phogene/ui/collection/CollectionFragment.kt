package com.hazemjday.phogene.ui.collection

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.hazemjday.phogene.Image
import com.hazemjday.phogene.R
import com.hazemjday.phogene.databinding.FragmentCollectionBinding

class CollectionFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef : DatabaseReference
    private lateinit var imagesRef: DatabaseReference
    private var quit = false
    private var _binding: FragmentCollectionBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        auth= FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().reference.child("users")
        imagesRef = FirebaseDatabase.getInstance().reference.child("images").child(auth.uid!!)
        _binding = FragmentCollectionBinding.inflate(inflater, container, false)
        val root: View = binding.root



        val linearLayout = binding.imagesContainer
        val query = imagesRef.orderByKey().limitToFirst(12)

        imagesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                if(count > 0){
                    query.addChildEventListener(object : ChildEventListener {
                        var contentCleared = false
                        @SuppressLint("InflateParams")
                        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                            val data = snapshot.getValue<HashMap<String, Any>>()
                            val image = Image(
                                prompt = data?.get("prompt") as String,
                                negative = data["negative"] as String,
                                width = data["width"] as Number,
                                height = data["height"] as Number,
                                steps = data["steps"] as Number,
                                guidance = data["guidance"] as Number,
                                sampler = data["sampler"] as String,
                                data = data["data"] as String,
                            )
                            if(!quit){
                                if(!contentCleared){
                                    linearLayout.removeAllViews()
                                    contentCleared =true
                                }
                                val v = LayoutInflater.from(context).inflate(R.layout.collection_image, null)
                                v.findViewById<TextView>(R.id.image_name).text = image.prompt
                                val decodedString = Base64.decode(image.data, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                v.findViewById<ImageView>(R.id.imageDrawable).setImageBitmap(bitmap)
                                v.setOnClickListener {
                                    showImageInDialog(bitmap, image.prompt)
                                }
                                linearLayout.addView(v, 0)
                            }

                        }
                        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                        override fun onChildRemoved(snapshot: DataSnapshot) {}
                        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
                else{
                    binding.waitForCollection.progressBarLoadingCollection.visibility = View.GONE
                    binding.waitForCollection.loading.text = "Collection is empty !"
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })



        return root
    }

    @SuppressLint("InflateParams")
    fun showImageInDialog(bitmap: Bitmap, prompt : String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_image, null)
        dialogView.findViewById<TextView>(R.id.dialog_prompt).text = prompt
        val dialogImageView = dialogView.findViewById<ImageView>(R.id.dialog_image_view)
        dialogImageView.setImageBitmap(bitmap)
        val dialog = activity?.let { Dialog(it) }
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog?.setContentView(dialogView)
        dialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        dialog?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        quit = true
        _binding = null
    }
}