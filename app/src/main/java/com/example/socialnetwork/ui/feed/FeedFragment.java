package com.example.socialnetwork.ui.feed;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.socialnetwork.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

/**
 * Главный экран ленты: заголовок, вкладки и ViewPager2.
 */
public class FeedFragment extends Fragment {

    private ViewPager2 pager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TabLayout tabs = view.findViewById(R.id.feed_tabs);
        pager = view.findViewById(R.id.feed_pager);

        FeedPagerAdapter adapter = new FeedPagerAdapter(this);
        pager.setAdapter(adapter);
        new TabLayoutMediator(tabs, pager, (tab, position) ->
                tab.setText(getString(FeedPagerAdapter.TITLES[position]))).attach();
    }

    /**
     * Перезагрузка всех страниц ленты (например, после создания публикации).
     */
    public void refreshAll() {
        if (pager == null) {
            return;
        }
        List<Fragment> fragments = getChildFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof PostsPageFragment) {
                ((PostsPageFragment) fragment).refresh();
            }
        }
    }
}
