package com.example.newsapp3.ui.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newsapp3.R
import com.example.newsapp3.adapter.NewsAdapter
import com.example.newsapp3.databinding.FragmentSearchBinding
import com.example.newsapp3.ui.NewsActivity
import com.example.newsapp3.ui.NewsViewModel
import com.example.newsapp3.utils.Constants
import com.example.newsapp3.utils.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.Serializable

class SearchFragment : Fragment(R.layout.fragment_search) {

    lateinit var newsViewModel: NewsViewModel
    lateinit var newsAdapter: NewsAdapter
    lateinit var retryButton: Button
    lateinit var tvError: TextView
    lateinit var itemSearchError: CardView
    lateinit var binding: FragmentSearchBinding

//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//
//        return inflater.inflate(R.layout.fragment_search, container, false)
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSearchBinding.bind(view)

        itemSearchError = view.findViewById(R.id.itemSearchError)
        var inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.item_error, null)
        retryButton = view.findViewById(R.id.btnRetry)
        tvError = view.findViewById(R.id.tvError)

        newsViewModel = (activity as NewsActivity).newsViewModel
        setUpSearchLinesRecyclerView()

        newsAdapter.setOnItemClickListener {
            val bundle = Bundle().apply {
                putString("article", it.toString())
            }
            findNavController().navigate(R.id.action_searchFragment2_to_articleFragment)
        }

        var job: Job? = null
        binding.editTextText.addTextChangedListener {
            editableText ->
            job?.cancel()
            job = MainScope().launch {
                delay(Constants.SEARCH_QUERY_TIME_DELAY)
                editableText?.let {
                    if (editableText.toString().isNotEmpty()) {
                        newsViewModel.searchNews(editableText.toString())
                    }
                }
            }
        }

        newsViewModel.headlines.observe(viewLifecycleOwner, { response ->
            when(response) {
                is Resource.Success<*> -> {
                    hideProgressBar()
                    hideErrorMessage()
                    response.data?.let {
                        newsResponse ->
                        newsAdapter.differ.submitList(newsResponse.articles)
                        val totalPages = newsResponse.totalResults / Constants.QUERY_PAGE_SIZE
                        val latePage = newsViewModel.searchNewsPage == totalPages
                        if(isLastPage) {
                            binding.recyclerViewSearch.setPadding(0,0,0,0)
                        }
                    }
                }

                is Resource.Error -> {
                    hideProgressBar()
                    response.message?.let {
                            message ->
                        Toast.makeText(activity, "Sorry error : $message", Toast.LENGTH_SHORT).show()
                        showErrorMessage(message)
                    }
                }
                is Resource.Loading -> {
                    showProgressBar()
                }
            }
        })

        newsViewModel.headlines.observe(viewLifecycleOwner, Observer {
                response ->
            when(response) {
                is Resource.Success<*> -> {
                    hideProgressBar()
                    hideErrorMessage()
                    response.data?.let {
                            newsResponse ->
                        newsAdapter.differ.submitList(newsResponse.articles.toList())
                        val totalPages = newsResponse.totalResults / Constants.QUERY_PAGE_SIZE
                        isLastPage = newsViewModel.headlinesPage == totalPages
                        if (isLastPage) {
                            binding.recyclerViewSearch.setPadding(0,0,0,0)
                        }
                    }
                }
                is Resource.Error<*> -> {
                    hideProgressBar()
                    response.message?.let {
                            message ->
                        Toast.makeText(activity, "Sorry error : $message", Toast.LENGTH_SHORT).show()
                        showErrorMessage(message)
                    }
                }
                is Resource.Loading -> {
                    showProgressBar()
                }
            }

            retryButton.setOnClickListener {
                if(binding.editTextText.text.toString().isNotEmpty()) {
                    newsViewModel.searchNews(binding.editTextText.text.toString())
                }
                else {
                    hideErrorMessage()
                }
            }
        })
    }

    var isLoading = false
    var isError = false
    var isLastPage = false
    var isScrolling = false

    private fun hideProgressBar() {
        binding.pagegnationProgressBar.visibility = View.INVISIBLE
        isLoading = false
    }

    private fun showProgressBar() {
        binding.pagegnationProgressBar.visibility = View.VISIBLE
        isLoading = true
    }

    private fun showErrorMessage(message: String) {
        itemSearchError.visibility = View.VISIBLE
        tvError.text = message
        isError = true
    }

    private fun hideErrorMessage() {
        itemSearchError.visibility = View.INVISIBLE
        isError = false
    }

    val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
            val visibleItemCount = layoutManager.childCount
            val totalItemCount = layoutManager.itemCount
            val isNoError = !isError
            val isNotLoadingAndNotLastPage = !isLoading && !isLastPage
            val isAtLastItem = firstVisibleItemPosition + visibleItemCount >= totalItemCount
            val isNotAtBeginning = firstVisibleItemPosition >= 0
            val isTotalMoreThanVisible = totalItemCount >= Constants.QUERY_PAGE_SIZE
            val shouldPaginate = isNoError && isNotLoadingAndNotLastPage && isAtLastItem && isNotAtBeginning && isTotalMoreThanVisible && isScrolling
            if(shouldPaginate) {
                newsViewModel.searchNews(binding.editTextText.text.toString())
                isScrolling = false
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if(newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL)
            {
                isScrolling = true
            }
        }
    }

    private fun setUpSearchLinesRecyclerView() {
        newsAdapter = NewsAdapter()
        binding.recyclerViewSearch.apply {
            adapter = newsAdapter
            layoutManager = LinearLayoutManager(activity)
            addOnScrollListener(this@SearchFragment.scrollListener)
        }
    }
}