package com.example.socialnetwork.ui.feed;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.socialnetwork.R;
import com.example.socialnetwork.data.PostRepository;

/**
 * Адаптер ViewPager2: три независимых страницы ленты.
 */
public class FeedPagerAdapter extends FragmentStateAdapter {

    private static final PostRepository.FeedType[] TYPES = {
            PostRepository.FeedType.ALL,
            PostRepository.FeedType.FOLLOWING,
            PostRepository.FeedType.POPULAR
    };

    public static final int[] TITLES = {
            R.string.tab_all,
            R.string.tab_following,
            R.string.tab_popular
    };

    public FeedPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return PostsPageFragment.newInstance(TYPES[position]);
    }

    @Override
    public int getItemCount() {
        return TYPES.length;
    }
}
