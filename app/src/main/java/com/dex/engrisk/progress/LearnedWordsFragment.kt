package com.dex.engrisk.progress

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.dex.engrisk.progress.adapter.LearnedWordAdapter
import com.dex.engrisk.databinding.FragmentLearnedWordsBinding
import com.dex.engrisk.model.UserProgress
import com.dex.engrisk.model.Vocabulary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class LearnedWordsFragment : Fragment() {
    private val TAG = "LearnedWordsFragment"
    private lateinit var binding: FragmentLearnedWordsBinding
    private lateinit var learnedWordAdapter: LearnedWordAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLearnedWordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        fetchLearnedWords()
    }

    private fun setupRecyclerView() {
        learnedWordAdapter = LearnedWordAdapter(emptyList())
        binding.rvLearnedWords.apply {
            adapter = learnedWordAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun fetchLearnedWords() {
        binding.progressBar.visibility = View.VISIBLE
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("userProgress").document(uid).get()
            .addOnSuccessListener { document ->
                val userProgress = document.toObject(UserProgress::class.java)
                val learnedWordsMap = userProgress?.vocabularyProgress?.filter { it.value.isLearned }

                if (!learnedWordsMap.isNullOrEmpty()) {
                    fetchWordDetails(learnedWordsMap.keys.toList())
                } else {
                    binding.progressBar.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Error fetching user progress for words", it)
            }
    }

    private fun fetchWordDetails(wordIds: List<String>) {
        db.collection("vocabulary").whereIn(FieldPath.documentId(), wordIds).get()
            .addOnSuccessListener { wordSnapshots ->
                val learnedWords = wordSnapshots.toObjects(Vocabulary::class.java)
                learnedWordAdapter.updateData(learnedWords)
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Error fetching word details", it)
            }
    }
}