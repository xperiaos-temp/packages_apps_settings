/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.fuelgauge.batterytip;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.batterytip.detectors.BatteryTipDetector;
import com.android.settings.fuelgauge.batterytip.detectors.LowBatteryDetector;
import com.android.settings.fuelgauge.batterytip.detectors.SummaryDetector;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.LowBatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.SummaryTip;
import com.android.settingslib.utils.AsyncLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loader to compute and return a battery tip list. It will always return a full length list even
 * though some tips may have state {@code BaseBatteryTip.StateType.INVISIBLE}.
 */
public class BatteryTipLoader extends AsyncLoader<List<BatteryTip>> {
    private static final String TAG = "BatteryTipLoader";

    private static final boolean USE_FAKE_DATA = false;

    private BatteryStatsHelper mBatteryStatsHelper;
    private BatteryUtils mBatteryUtils;
    @VisibleForTesting
    int mVisibleTips;

    public BatteryTipLoader(Context context, BatteryStatsHelper batteryStatsHelper) {
        super(context);
        mBatteryStatsHelper = batteryStatsHelper;
        mBatteryUtils = BatteryUtils.getInstance(context);
    }

    @Override
    public List<BatteryTip> loadInBackground() {
        if (USE_FAKE_DATA) {
            return getFakeData();
        }
        final List<BatteryTip> tips = new ArrayList<>();
        final BatteryTipPolicy policy = new BatteryTipPolicy(getContext());
        final BatteryInfo batteryInfo = mBatteryUtils.getBatteryInfo(mBatteryStatsHelper, TAG);
        mVisibleTips = 0;

        addBatteryTipFromDetector(tips, new LowBatteryDetector(policy, batteryInfo));
        // Add summary detector at last since it need other detectors to update the mVisibleTips
        addBatteryTipFromDetector(tips, new SummaryDetector(policy, mVisibleTips));

        Collections.sort(tips);
        return tips;
    }

    @Override
    protected void onDiscardResult(List<BatteryTip> result) {
    }

    private List<BatteryTip> getFakeData() {
        final List<BatteryTip> tips = new ArrayList<>();
        tips.add(new SummaryTip(BatteryTip.StateType.NEW));
        tips.add(new LowBatteryTip(BatteryTip.StateType.NEW));

        return tips;
    }

    @VisibleForTesting
    void addBatteryTipFromDetector(final List<BatteryTip> tips,
            final BatteryTipDetector detector) {
        final BatteryTip batteryTip = detector.detect();
        mVisibleTips += batteryTip.isVisible() ? 1 : 0;
        tips.add(batteryTip);
    }

}