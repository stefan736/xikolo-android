package de.xikolo.controllers.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.yatatsu.autobundle.AutoBundleField;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import butterknife.BindView;
import de.xikolo.R;
import de.xikolo.controllers.course.CourseActivityAutoBundle;
import de.xikolo.controllers.helper.CourseListFilter;
import de.xikolo.controllers.login.LoginActivityAutoBundle;
import de.xikolo.events.LoginEvent;
import de.xikolo.events.LogoutEvent;
import de.xikolo.models.Course;
import de.xikolo.presenters.base.PresenterFactory;
import de.xikolo.presenters.main.CourseListFilterAllPresenterFactory;
import de.xikolo.presenters.main.CourseListFilterMyPresenterFactory;
import de.xikolo.presenters.main.CourseListPresenter;
import de.xikolo.presenters.main.CourseListView;
import de.xikolo.utils.SectionList;
import de.xikolo.views.AutofitRecyclerView;
import de.xikolo.views.SpaceItemDecoration;

public class CourseListFragment extends PresenterMainFragment<CourseListPresenter, CourseListView> implements CourseListView {

    public static final String TAG = CourseListFragment.class.getSimpleName();

    @AutoBundleField CourseListFilter filter;

    @BindView(R.id.content_view) AutofitRecyclerView recyclerView;

    private CourseListAdapter courseListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        EventBus.getDefault().register(this);
    }

    @Override
    public int getLayoutResource() {
        return R.layout.content_course_list;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        courseListAdapter = new CourseListAdapter(this, new CourseListAdapter.OnCourseButtonClickListener() {
            @Override
            public void onEnrollButtonClicked(String courseId) {
                presenter.onEnrollButtonClicked(courseId);
            }

            @Override
            public void onContinueButtonClicked(String courseId) {
                presenter.onCourseEnterButtonClicked(courseId);
            }

            @Override
            public void onDetailButtonClicked(String courseId) {
                presenter.onCourseDetailButtonClicked(courseId);
            }
        }, filter);

        recyclerView.setAdapter(courseListAdapter);

        recyclerView.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return courseListAdapter.isHeader(position) ? recyclerView.getSpanCount() : 1;
            }
        });

        recyclerView.addItemDecoration(new SpaceItemDecoration(
                getActivity().getResources().getDimensionPixelSize(R.dimen.card_horizontal_margin),
                getActivity().getResources().getDimensionPixelSize(R.dimen.card_vertical_margin),
                false,
                new SpaceItemDecoration.RecyclerViewInfo() {
                    @Override
                    public boolean isHeader(int position) {
                        return courseListAdapter.isHeader(position);
                    }

                    @Override
                    public int getSpanCount() {
                        return recyclerView.getSpanCount();
                    }

                    @Override
                    public int getItemCount() {
                        return courseListAdapter.getItemCount();
                    }
                }));
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getActivityCallback() != null) {
            if (filter == CourseListFilter.ALL) {
                getActivityCallback().onFragmentAttached(NavigationAdapter.NAV_ALL_COURSES.getPosition(), getString(R.string.title_section_all_courses));
            } else {
                getActivityCallback().onFragmentAttached(NavigationAdapter.NAV_MY_COURSES.getPosition(), getString(R.string.title_section_my_courses));
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void showCourseList(SectionList<String, List<Course>> courseList) {
        if (courseListAdapter != null) {
            courseListAdapter.update(courseList);
        }
    }

    @Override
    public void enterCourse(String courseId) {
        Intent intent = CourseActivityAutoBundle.builder().courseId(courseId).build(getActivity());
        startActivity(intent);
    }

    @Override
    public void enterCourseDetails(String courseId) {
        Intent intent = CourseActivityAutoBundle.builder().courseId(courseId).build(getActivity());
        startActivity(intent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (getActivityCallback() != null && !getActivityCallback().isDrawerOpen()) {
            inflater.inflate(R.menu.refresh, menu);
            inflater.inflate(R.menu.search, menu);

            MenuItem searchMenuItem = menu.findItem(R.id.search);
            searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    menu.removeItem(R.id.action_refresh);
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    if (getActivity() != null) {
                        getActivity().invalidateOptionsMenu();
                    }
                    return true;
                }
            });
            SearchView searchView = (SearchView) searchMenuItem.getActionView();
            searchView.setIconifiedByDefault(false);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    presenter.onSearch(query, filter == CourseListFilter.MY);
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    presenter.onSearch(newText, filter == CourseListFilter.MY);
                    return false;
                }
            });

            super.onCreateOptionsMenu(menu, inflater);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.action_refresh:
                onRefresh();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void openLogin() {
        Intent intent = LoginActivityAutoBundle.builder().build(getActivity());
        startActivity(intent);
    }

    @NonNull
    @Override
    protected PresenterFactory<CourseListPresenter> getPresenterFactory() {
        return filter == CourseListFilter.ALL ? new CourseListFilterAllPresenterFactory() : new CourseListFilterMyPresenterFactory();
    }

    @Override
    public void showLoginRequiredMessage() {
        super.showLoginRequiredMessage();
        loadingStateHelper.setMessageOnClickListener((v) -> getActivityCallback()
                .selectDrawerSection(NavigationAdapter.NAV_PROFILE.getPosition()));
    }

    @Override
    public void showNoEnrollmentsMessage() {
        loadingStateHelper.setMessageTitle(R.string.notification_no_enrollments);
        loadingStateHelper.setMessageSummary(R.string.notification_no_enrollments_summary);
        loadingStateHelper.setMessageOnClickListener((v) -> getActivityCallback()
                .selectDrawerSection(NavigationAdapter.NAV_ALL_COURSES.getPosition()));
        loadingStateHelper.showMessage();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLoginEvent(LoginEvent event) {
        if (presenter != null) {
            presenter.onRefresh();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLogoutEvent(LogoutEvent event) {
        if (presenter != null) {
            presenter.onRefresh();
        }
    }

}
