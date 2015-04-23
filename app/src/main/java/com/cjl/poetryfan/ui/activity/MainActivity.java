package com.cjl.poetryfan.ui.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;

import com.cjl.poetryfan.R;
import com.cjl.poetryfan.ui.fragment.NavDrawerFragment;
import com.cjl.poetryfan.ui.presenter.MainPresenter;
import com.cjl.poetryfan.util.PreferenceUtil;

import java.util.List;

import javax.inject.Inject;

import butterknife.InjectView;

public class MainActivity extends BaseActivity implements MainPresenter.MainView {

    @Inject
    MainPresenter mPresenter;

    @InjectView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TypedValue sTypedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorPrimaryDark, sTypedValue, true);
        mDrawerLayout.setStatusBarBackgroundColor(sTypedValue.data);

        mPresenter.setView(this);
    }

    @Override
    public void setSupportActionBar(final Toolbar toolbar) {
        super.setSupportActionBar(toolbar);

        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                NavDrawerFragment mNavDrawerFragment = (NavDrawerFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.navigation_drawer);
                mNavDrawerFragment.setUp(R.id.navigation_drawer, mDrawerLayout, toolbar);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.setView(null);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_main;
    }

    @Override
    protected List<Object> getExtraModules() {
        return null;
    }

    @Override
    public void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }
}
