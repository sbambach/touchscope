/*
 * MIT License
 *
 * Copyright (c) 2016 Matthew Heinz
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.uni_weimar.mheinz.androidtouchscope;

import android.graphics.Color;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.support.v7.widget.Toolbar;

import de.uni_weimar.mheinz.androidtouchscope.display.HostView;
import de.uni_weimar.mheinz.androidtouchscope.display.callback.OnDataChangedInterface;
import de.uni_weimar.mheinz.androidtouchscope.scope.*;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.TimeData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.TriggerData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.WaveData;

// TODO: Measure and Cursor options in left drawer
public class TouchScopeActivity extends AppCompatActivity
{
    private static final String TAG = "TouchScopeActivity";
    private static final int REFRESH_RATE = 100;

    private ScopeInterface mActiveScope = null;
   // private ScopeView mScopeView = null;
    private HostView mHostView = null;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mLeftDrawerToggle;
    NavigationView mLeftDrawer;

    private final Handler mRefreshHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touch_scope);

        Toolbar toolbar = (Toolbar)findViewById(R.id.scope_toolbar);
        setSupportActionBar(toolbar);

        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        assert mDrawerLayout != null;
        mDrawerLayout.setScrimColor(Color.TRANSPARENT);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        mLeftDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close);
        mDrawerLayout.addDrawerListener(mLeftDrawerToggle);

    /*    mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        assert toolbar != null;
        toolbar.setNavigationOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
        });*/

        mLeftDrawer = (NavigationView)findViewById(R.id.left_drawer);
        assert mLeftDrawer != null;
        mLeftDrawer.setNavigationItemSelectedListener(mLeftDrawerSelectedListener);

        /*NavigationView rightDrawer = (NavigationView)findViewById(R.id.right_drawer);
        assert rightDrawer != null;
        rightDrawer.setNavigationItemSelectedListener(mRightDrawerSelectedListener);*/

        mHostView = (HostView)findViewById(R.id.hostView);
        mHostView.setOnDoCommand(new OnDataChangedInterface.OnDataChanged()
        {
            @Override
            public void doCommand(ScopeInterface.Command command, int channel, Object specialData)
            {
                if(mActiveScope != null)
                {
                    mActiveScope.doCommand(command, channel, true, specialData);
                }
            }
        });

        ToggleButton runStopButton = (ToggleButton)findViewById(R.id.buttonRunStop);
        assert runStopButton != null;
        runStopButton.setChecked(true);

        // test if it is emulator
        initScope(!Build.FINGERPRINT.contains("generic"));
    }

    private void initScope(boolean doReal)
    {
        Toolbar toolbar = (Toolbar) findViewById(R.id.scope_toolbar);
        assert toolbar != null;
        ((TextView) toolbar.findViewById(R.id.toolbar_title)).setText(R.string.app_name);

        if(mActiveScope != null)
        {
            mRefreshHandler.removeCallbacks(mRefreshRunnable);
            mActiveScope.close();
        }

        if(doReal)
        {
            Log.i(TAG, "Device detected, try to find RigolScope");
            mActiveScope = new RigolScope(this);
            mLeftDrawer.getMenu().getItem(0).setChecked(true);
        }
        else
        {
            Log.i(TAG, "Emulator detected, using TestScope");
            mActiveScope = new TestScope();
            mLeftDrawer.getMenu().getItem(1).setChecked(true);
        }
        mActiveScope.open(new ScopeInterface.OnReceivedName()
        {
            @Override
            public void returnName(String name)
            {
                final String scopeName = name;
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toolbar toolbar = (Toolbar) findViewById(R.id.scope_toolbar);
                        assert toolbar != null;
                        ((TextView) toolbar.findViewById(R.id.toolbar_title)).setText(scopeName);
                    }
                });
            }
        });
    }

    private void startRunnableAndScope()
    {
        mActiveScope.start();
        new Thread(new Runnable()
        {
            public void run()
            {
                if (mActiveScope != null)
                {
                    mRefreshHandler.removeCallbacks(mRefreshRunnable);
                    mRefreshHandler.postDelayed(mRefreshRunnable, 0);

                }
            }
        }).start();
    }

    @Override
    public void onDestroy()
    {
        mRefreshHandler.removeCallbacks(mRefreshRunnable);

        if(mActiveScope != null)
            mActiveScope.close();
        mActiveScope = null;

        super.onDestroy();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mRefreshHandler.removeCallbacks(mRefreshRunnable);
        if(mActiveScope != null)
            mActiveScope.stop();//.close();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if(mActiveScope == null)
        {
            initScope(!Build.FINGERPRINT.contains("generic"));
        }

        startRunnableAndScope();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_touch_scope, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (mLeftDrawerToggle.onOptionsItemSelected(item))
        {
            return true;
        }

       /* int id = item.getItemId();
        if(id == R.id.action_rightDrawer)
        {
            mDrawerLayout.openDrawer(GravityCompat.END);
        }
        else
        {
            mDrawerLayout.closeDrawer(GravityCompat.END);
        }*/

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mLeftDrawerToggle.syncState();
    }

    private final NavigationView.OnNavigationItemSelectedListener mLeftDrawerSelectedListener =
            new NavigationView.OnNavigationItemSelectedListener()
    {
        @Override
        public boolean onNavigationItemSelected(MenuItem item)
        {
            mDrawerLayout.closeDrawers();

            item.setChecked(!item.isChecked());

            switch (item.getItemId())
            {
                case R.id.navigation_real:
                    initScope(true);
                    break;
                case R.id.navigation_test:
                    initScope(false);
                    break;
            }
            startRunnableAndScope();
            return true;
        }
    };

   /* private final NavigationView.OnNavigationItemSelectedListener mRightDrawerSelectedListener =
            new NavigationView.OnNavigationItemSelectedListener()
    {
        @Override
        public boolean onNavigationItemSelected(MenuItem item)
        {
            //Closing drawer on item click
            mDrawerLayout.closeDrawers();

            if (mActiveScope == null)
                return true;

            item.setChecked(!item.isChecked());

            //Check to see which item was being clicked and perform appropriate action
            switch (item.getItemId())
            {
                case R.id.navigation_channel1:
                    mActiveScope.doCommand(
                            ScopeInterface.Command.SET_CHANNEL_STATE,
                            1,
                            true,
                            (Boolean) item.isChecked());
                    break;
                case R.id.navigation_channel2:
                    mActiveScope.doCommand(
                            ScopeInterface.Command.SET_CHANNEL_STATE,
                            2,
                            true,
                            (Boolean) item.isChecked());
                    break;
            }
            return false;
        }
    };*/

    private final Runnable mRefreshRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            TimeData timeData = mActiveScope.getTimeData();
            TriggerData trigData = mActiveScope.getTriggerData();

            WaveData waveData = mActiveScope.getWave(1);
            mHostView.setChannelData(1, waveData,timeData, trigData);

            waveData = mActiveScope.getWave(2);
            mHostView.setChannelData(2, waveData,timeData, trigData);

            mRefreshHandler.postDelayed(this, REFRESH_RATE);
        }
    };

    public void onRunStop(View view)
    {
        final boolean isChecked = ((ToggleButton)view).isChecked();

        if(mActiveScope != null)
            mActiveScope.doCommand(
                    ScopeInterface.Command.SET_RUN_STOP,
                    0,
                    true,
                    isChecked);
    }

    public void onAuto(View view)
    {
        final ToggleButton button = (ToggleButton)view;
        Handler handler = new Handler();
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                if(mActiveScope != null)
                    mActiveScope.doCommand(
                            ScopeInterface.Command.DO_AUTO,
                            0,
                            true,
                            button.isChecked());

                Log.i(TAG, "Auto Completed");
                button.setChecked(false);
            }
        }, 0);
    }
}
