package com.example.poetry.features.favorite.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poetry.R
import com.example.poetry.core.auth.SessionManager
import com.example.poetry.core.ui.VerticalSpaceItemDecoration
import com.example.poetry.core.util.dp
import com.example.poetry.databinding.FragmentFavoriteListBinding
import com.example.poetry.features.favorite.adapter.FavoritePostAdapter
import com.example.poetry.features.favorite.model.FavoritePostUiModel
import com.example.poetry.features.favorite.viewmodel.FavoriteViewModel
import com.example.poetry.features.poem.adapter.PoemSummaryAdapter
import com.example.poetry.features.poem.model.PoemSummaryUiModel
import com.google.android.material.tabs.TabLayout

class FavoriteListFragment : Fragment(R.layout.fragment_favorite_list) {

    private var _binding: FragmentFavoriteListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoriteViewModel by viewModels()
    private lateinit var poemAdapter: PoemSummaryAdapter
    private lateinit var postAdapter: FavoritePostAdapter

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val searchDebounceMs = 400L

    private val tabListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            onFavoriteTabChanged()
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {}

        override fun onTabReselected(tab: TabLayout.Tab?) {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFavoriteListBinding.bind(view)

        poemAdapter = PoemSummaryAdapter(
            showRemoveFavorite = true,
            onRemoveFavorite = { item -> confirmRemovePoemFavorite(item) },
            onClick = {
                findNavController().navigate(
                    R.id.action_myFavorites_to_poemDetail,
                    bundleOf("workId" to it.workId)
                )
            }
        )

        postAdapter = FavoritePostAdapter(
            showRemove = true,
            onRemove = { item -> confirmRemovePostCollect(item) },
            onClick = {
                findNavController().navigate(
                    R.id.action_myFavorites_to_postDetail,
                    bundleOf("post_id" to it.postId)
                )
            }
        )

        binding.favoriteRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = poemAdapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(10)))
        }

        binding.favoriteTabLayout.addTab(binding.favoriteTabLayout.newTab().setText("诗词"))
        binding.favoriteTabLayout.addTab(binding.favoriteTabLayout.newTab().setText("帖子"))
        binding.favoriteTabLayout.addOnTabSelectedListener(tabListener)

        binding.favoriteSearchInput.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                v.hideKeyboard()
                scheduleReload(resetDebounce = false)
                true
            } else {
                false
            }
        }

        binding.favoriteSearchInput.doAfterTextChanged {
            scheduleReload(resetDebounce = true)
        }

        binding.emptyGoLoginButton.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }
        binding.emptyBrowseHomeButton.setOnClickListener {
            if (isPoemTab()) {
                findNavController().popBackStack(R.id.homeFragment, false)
            } else {
                findNavController().navigate(R.id.communitySquareFragment)
            }
        }
        binding.emptyGoSearchButton.setOnClickListener {
            findNavController().navigate(R.id.searchResultFragment)
        }
        binding.emptyClearSearchButton.setOnClickListener {
            binding.favoriteSearchInput.setText("")
            scheduleReload(resetDebounce = false)
        }

        viewModel.favoriteList.observe(viewLifecycleOwner) { poems ->
            poemAdapter.submitList(poems)
            updateEmptyAndListVisibility()
        }
        viewModel.favoritePostList.observe(viewLifecycleOwner) { posts ->
            postAdapter.submitList(posts)
            updateEmptyAndListVisibility()
        }
        viewModel.isLoading.observe(viewLifecycleOwner) {
            updateEmptyAndListVisibility()
        }
        viewModel.loadError.observe(viewLifecycleOwner) { err ->
            updateEmptyAndListVisibility()
            err?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        updateSearchHint()
    }

    override fun onResume() {
        super.onResume()
        scheduleReload(resetDebounce = false)
    }

    override fun onDestroyView() {
        binding.favoriteTabLayout.removeOnTabSelectedListener(tabListener)
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        searchRunnable = null
        super.onDestroyView()
        _binding = null
    }

    private fun isPoemTab(): Boolean = binding.favoriteTabLayout.selectedTabPosition == 0

    private fun onFavoriteTabChanged() {
        binding.favoriteRecycler.adapter = if (isPoemTab()) poemAdapter else postAdapter
        updateSearchHint()
        scheduleReload(resetDebounce = false)
        updateEmptyAndListVisibility()
    }

    private fun updateSearchHint() {
        binding.favoriteSearchLayout.hint = if (isPoemTab()) {
            "搜索收藏（标题、作者、正文关键词）"
        } else {
            "搜索帖子（标题、正文、话题、作者）"
        }
    }

    private fun scheduleReload(resetDebounce: Boolean) {
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        val run = Runnable { reloadFromServer() }
        searchRunnable = run
        if (resetDebounce) {
            searchHandler.postDelayed(run, searchDebounceMs)
        } else {
            searchHandler.post(run)
        }
    }

    private fun reloadFromServer() {
        val session = SessionManager(requireContext())
        val auth = session.bearerAuthorization()
        val q = binding.favoriteSearchInput.text?.toString()
        if (isPoemTab()) {
            viewModel.loadMyFavorites(auth, q)
        } else {
            viewModel.loadMyPostCollects(auth, q)
        }
    }

    private fun updateEmptyAndListVisibility() {
        val session = SessionManager(requireContext())
        val loggedIn = session.isLoggedIn()
        val hasQuery = binding.favoriteSearchInput.text?.toString()?.trim().orEmpty().isNotEmpty()
        val poemTab = isPoemTab()
        val poems = viewModel.favoriteList.value.orEmpty()
        val posts = viewModel.favoritePostList.value.orEmpty()
        val hasRows = if (poemTab) poems.isNotEmpty() else posts.isNotEmpty()
        val loading = viewModel.isLoading.value == true
        val err = viewModel.loadError.value

        binding.loadingProgress.isVisible = loading

        binding.favoriteSearchLayout.isEnabled = loggedIn
        binding.favoriteSearchInput.isEnabled = loggedIn

        when {
            loading && !hasRows -> {
                binding.favoriteRecycler.isVisible = false
                binding.emptyState.isVisible = false
            }
            hasRows -> {
                binding.favoriteRecycler.isVisible = true
                binding.emptyState.isVisible = false
            }
            else -> {
                binding.favoriteRecycler.isVisible = false
                binding.emptyState.isVisible = true
            }
        }

        binding.emptyGoLoginButton.isVisible = !loggedIn
        binding.emptyBrowseHomeButton.isVisible = loggedIn && !hasQuery && err == null
        binding.emptyGoSearchButton.isVisible = loggedIn && !hasQuery && err == null && poemTab
        binding.emptyClearSearchButton.isVisible = loggedIn && hasQuery

        binding.emptyBrowseHomeButton.text = if (poemTab) "去首页发现诗词" else "去内容广场"

        binding.emptyText.text = when {
            loading && !hasRows -> "加载中…"
            !loggedIn -> "请先登录后查看与管理收藏"
            err != null -> if (poemTab) "无法加载诗词收藏列表" else "无法加载帖子收藏列表"
            hasQuery -> if (poemTab) "未找到匹配的收藏" else "未找到匹配的帖子收藏"
            else -> if (poemTab) "暂无收藏，去发现喜欢的诗词吧" else "暂无帖子收藏，去内容广场逛逛吧"
        }
    }

    private fun confirmRemovePoemFavorite(item: PoemSummaryUiModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("取消收藏")
            .setMessage("确定从收藏中移除「${item.title}」吗？")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("移除") { _, _ ->
                val auth = SessionManager(requireContext()).bearerAuthorization()
                viewModel.removeFavorite(auth, item.workId) { ok ->
                    if (ok) {
                        Toast.makeText(requireContext(), "已取消收藏", Toast.LENGTH_SHORT).show()
                        reloadFromServer()
                    } else {
                        Toast.makeText(requireContext(), "操作失败，请稍后重试", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun confirmRemovePostCollect(item: FavoritePostUiModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("取消收藏")
            .setMessage("确定从收藏中移除「${item.title}」吗？")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("移除") { _, _ ->
                val auth = SessionManager(requireContext()).bearerAuthorization()
                viewModel.removePostCollect(auth, item.postId) { ok ->
                    if (ok) {
                        Toast.makeText(requireContext(), "已取消收藏", Toast.LENGTH_SHORT).show()
                        reloadFromServer()
                    } else {
                        Toast.makeText(requireContext(), "操作失败，请稍后重试", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun View.hideKeyboard() {
        val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
            as? android.view.inputmethod.InputMethodManager
        imm?.hideSoftInputFromWindow(windowToken, 0)
    }
}
