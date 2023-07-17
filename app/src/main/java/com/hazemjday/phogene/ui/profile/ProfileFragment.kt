package com.hazemjday.phogene.ui.profile

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.hazemjday.phogene.AuthActivity
import com.hazemjday.phogene.User
import com.hazemjday.phogene.databinding.FragmentProfileBinding
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private lateinit var userRef: DatabaseReference
    private lateinit var user: User
    private lateinit var auth: FirebaseAuth
    var quit = false
    private lateinit var calendar : Calendar
    private val binding get() = _binding!!
    private lateinit var progressDialog: ProgressDialog


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        quit = false
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        progressDialog = ProgressDialog.show(context, "", "Loading user data..", true)

        auth= FirebaseAuth.getInstance()
        userRef = FirebaseDatabase.getInstance().reference.child("users").child(auth.currentUser!!.uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userValues = snapshot.value as Map<*, *>
                user = User(userValues["name"] as String,userValues["email"] as String, userValues["birthDate"] as String)



                if(!quit){
                    "Email : ${user.email}".also { binding.emailLabel.text = it }

                    binding.nameEdittext.setText(user.name)
                    binding.birthdayEditText.setText(user.birthDate)
                }

                progressDialog.dismiss()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        binding.editButton.setOnClickListener{
            Toast.makeText(context,"Saving user info..",Toast.LENGTH_SHORT).show()
            userRef.child("name").setValue(binding.nameEdittext.text.toString())
            userRef.child("birthDate").setValue(binding.birthdayEditText.text.toString())
        }

        binding.logoutButton.setOnClickListener{
            auth.signOut()
            requireActivity().finish()
            val intent = Intent(requireContext(), AuthActivity::class.java)
            startActivity(intent)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        quit = true
    }
}