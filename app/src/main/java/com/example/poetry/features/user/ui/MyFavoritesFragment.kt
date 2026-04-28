package com.example.poetry.features.user.ui

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.poetry.R
import com.example.poetry.core.auth.SessionManager
import com.example.poetry.core.ui.VerticalSpaceItemDecoration
import com.example.poetry.core.util.dp
import com.example.poetry.databinding.FragmentMyFavoritesBinding
import com.example.poetry.features.user.adapter.FavoriteListAdapter
import com.example.poetry.features.user.model.FavoriteItemUiModel
import com.example.poetry.features.user.model.FavoriteTypeUi
import com.example.poetry.features.user.viewmodel.UserViewModel
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputLayout

class MyFavoritesFragment : Fragment(R.layout.fragment_my_favorites) {

    private var _binding: FragmentMyFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserViewModel by viewModels()
    private lateinit var adapter: FavoriteListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyFavoritesBinding.bind(view)

        val session = SessionManager(requireContext())
        viewModel.setSessionToken(session.token())

        adapter = FavoriteListAdapter(
            onOpen = { openFavorite(it) },
            onUnfavorite = { unfavorite(it) }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MyFavoritesFragment.adapter
            addItemDecoration(VerticalSpaceItemDecoration(requireContext().dp(12)))
        }

        binding.typeToggle.check(R.id.tabPosts)
        binding.typeToggle.addOnButtonCheckedListener(
            MaterialButtonToggleGroup.OnButtonCheckedListener { _, checkedId, isChecked ->
                if (!isChecked) return@OnButtonCheckedListener
                when (checkedId) {
                    R.id.tabPosts -> viewModel.setFavoriteTab(UserViewModel.FavoriteTab.POSTS)
                    R.id.tabPoems -> viewModel.setFavoriteTab(UserViewModel.FavoriteTab.POEMS)
                }
                updateSearchHint(binding.searchInputLayout)
                updateEmptyText()
            }
        )

        binding.searchEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.setFavoriteQuery(binding.searchEdit.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }
        binding.searchEdit.addTextChangedListener(SimpleTextWatcher { text ->
            viewModel.setFavoriteQuery(text)
        })
        binding.searchButton.setOnClickListener {
            viewModel.setFavoriteQuery(binding.searchEdit.text?.toString().orEmpty())
        }

        binding.emptyActionButton.setOnClickListener {
            when (viewModel.favoriteTab.value ?: UserViewModel.FavoriteTab.POSTS) {
                UserViewModel.FavoriteTab.POSTS -> switchToBottomTab(R.id.communitySquareFragment)
                UserViewModel.FavoriteTab.POEMS -> switchToBottomTab(R.id.homeFragment)
            }
        }

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return
                val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val last = lm.findLastVisibleItemPosition()
                if (last >= adapter.itemCount - 2) {
                    viewModel.loadMoreFavorites()
                }
            }
        })

        viewModel.favoriteItems.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            updateEmptyState(list.isEmpty())
        }
        viewModel.favoriteCount.observe(viewLifecycleOwner) { count ->
            updateCountText(count, viewModel.favoriteShownCount.value ?: count)
        }
        viewModel.favoriteShownCount.observe(viewLifecycleOwner) { shown ->
            updateCountText(viewModel.favoriteCount.value ?: shown, shown)
        }
        viewModel.favoriteIsLoadingMore.observe(viewLifecycleOwner) { isLoading ->
            binding.loadMoreProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        viewModel.favoriteTab.observe(viewLifecycleOwner) {
            updateCountText(viewModel.favoriteCount.value ?: 0, viewModel.favoriteShownCount.value ?: 0)
            updateSearchHint(binding.searchInputLayout)
            updateEmptyText()
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun openFavorite(item: FavoriteItemUiModel) {
        when (item.type) {
            FavoriteTypeUi.POEM -> {
                val wid = item.linkedPoemWorkId
                if (wid != null) {
                    findNavController().navigate(
                        R.id.action_myFavorites_to_poemDetail,
                        Bundle().apply { putLong("workId", wid) }
                    )
                } else {
                    Toast.makeText(requireContext(), "暂无诗词详情", Toast.LENGTH_SHORT).show()
                }
            }
            FavoriteTypeUi.POST -> {
                // 目前详情页未接入真实帖子ID，这里直接进入详情页演示
                findNavController().navigate(R.id.postDetailFragment)
            }
        }
    }

    private fun unfavorite(item: FavoriteItemUiModel) {
        viewModel.unfavorite(item) { ok ->
            if (ok) Toast.makeText(requireContext(), "已取消收藏", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCountText(count: Int) {
        updateCountText(count, count)
    }

    private fun updateCountText(totalCount: Int, shownCount: Int) {
        val tab = viewModel.favoriteTab.value ?: UserViewModel.FavoriteTab.POSTS
        val base = when (tab) {
            UserViewModel.FavoriteTab.POSTS -> getString(R.string.favorite_count_posts_fmt, totalCount)
            UserViewModel.FavoriteTab.POEMS -> getString(R.string.favorite_count_poems_fmt, totalCount)
        }
        binding.countText.text =
            if ((viewModel.favoriteQuery.value ?: "").isBlank() || shownCount == totalCount) base
            else getString(R.string.favorite_count_filtered_fmt, base, shownCount)
    }

    private fun updateSearchHint(inputLayout: TextInputLayout) {
        val tab = viewModel.favoriteTab.value ?: UserViewModel.FavoriteTab.POSTS
        inputLayout.hint = when (tab) {
            UserViewModel.FavoriteTab.POSTS -> getString(R.string.favorite_search_hint_posts)
            UserViewModel.FavoriteTab.POEMS -> getString(R.string.favorite_search_hint_poems)
        }
    }

    private fun updateEmptyText() {
        val tab = viewModel.favoriteTab.value ?: UserViewModel.FavoriteTab.POSTS
        binding.emptyText.text = when (tab) {
            UserViewModel.FavoriteTab.POSTS -> getString(R.string.favorite_empty_posts)
            UserViewModel.FavoriteTab.POEMS -> getString(R.string.favorite_empty_poems)
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun switchToBottomTab(@IdRes destinationId: Int) {
        val bottomNav =
            activity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        if (bottomNav != null) {
            // 通过 BottomNavigationView 切换，避免在“个人中心”栈内打开导致返回/切换异常
            bottomNav.selectedItemId = destinationId
        } else {
            // 兜底：仍然走主 NavController 的顶层目的地
            findNavController().navigate(destinationId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class SimpleTextWatcher(
    private val onTextChanged: (String) -> Unit
) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
    override fun afterTextChanged(s: android.text.Editable?) {
        onTextChanged(s?.toString().orEmpty())
    }
}
