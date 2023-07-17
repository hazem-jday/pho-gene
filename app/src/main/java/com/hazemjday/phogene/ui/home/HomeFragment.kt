package com.hazemjday.phogene.ui.home

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
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
import com.hazemjday.phogene.databinding.FragmentHomeBinding
import com.hazemjday.phogene.*
import com.hazemjday.phogene.R
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var dbRef : DatabaseReference
    private lateinit var imagesRef: DatabaseReference
    private var quit = false
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        quit = false
        auth= FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().reference.child("users")
        imagesRef = FirebaseDatabase.getInstance().reference.child("images").child(auth.uid!!)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val userRef = dbRef.child(auth.currentUser!!.uid)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userData = dataSnapshot.getValue<HashMap<String, Any>>()
                val user = User(
                    name = userData?.get("name") as String,
                    email = userData["email"] as String,
                    birthDate = userData["birthDate"] as String
                )
                _binding?.welcome?.text = "Welcome, ${user.name}"
            }
            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
        val linearLayout = binding.imageCards


        imagesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                    val count = snapshot.childrenCount

                    if(count > 0){
                        val query = imagesRef.orderByKey().limitToLast(5)
                        query.addChildEventListener(object : ChildEventListener {
                            var contentCleared = false

                            @SuppressLint("InflateParams")
                            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

                                val data = snapshot.getValue<HashMap<String, Any>>()
                                val image = Image(
                                    prompt = data?.get("prompt") as String,
                                    negative = data?.get("negative") as String,
                                    width = data?.get("width") as Number,
                                    height = data?.get("height") as Number,
                                    steps = data?.get("steps") as Number,
                                    guidance = data?.get("guidance") as Number,
                                    sampler = data?.get("sampler") as String,
                                    data = data?.get("data") as String,
                                )
                                if(! quit){

                                    val v = LayoutInflater.from(context).inflate(R.layout.generated_image_card, null)
                                    v.findViewById<TextView>(R.id.image_name).text = SimpleDateFormat("dd/MM/yyyy", Locale("TN")).format(snapshot.key!!.toLong())
                                    val decodedString = Base64.decode(image.data, Base64.DEFAULT)
                                    val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                    v.findViewById<ImageView>(R.id.imageDrawable).setImageBitmap(bitmap)
                                    v.setOnClickListener {
                                        showImageInDialog(bitmap,image.prompt)
                                    }
                                    if(!contentCleared){
                                        linearLayout.removeAllViews()
                                        contentCleared =true
                                    }
                                    linearLayout.addView(v , 0)
                                }



                            }
                            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                            override fun onChildRemoved(snapshot: DataSnapshot) {}
                            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                            override fun onCancelled(error: DatabaseError) {}
                        })
                    }
                    else{
                        binding.noImageLoading.progressNoImage.visibility = View.GONE
                        binding.noImageLoading.imageName.text = "No recent images"
                    }

            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })




        binding.generateButton.setOnClickListener{
            startActivity(Intent(activity, GenerateActivity::class.java))
        }
        binding.learnMoreButton.setOnClickListener{
            startActivity(Intent(activity, LearnMoreActivity::class.java))
        }



        return root
    }


    @SuppressLint("InflateParams")
    fun showImageInDialog(bitmap: Bitmap, prompt: String) {
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