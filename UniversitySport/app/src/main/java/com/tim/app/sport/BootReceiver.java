/*
 * Copyright 2013 Thomas Hoffmann
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tim.app.sport;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
//
//        SharedPreferences prefs = context.getSharedPreferences("pedometer", Context.MODE_PRIVATE);
//
//        Database db = Database.getInstance(context);
//
//        if (!prefs.getBoolean("correctShutdown", false)) {
//            // can we at least recover some steps?
//            int steps = db.getCurrentSteps();
//            db.addToLastEntry(steps);
//        }
//        // last entry might still have a negative step value, so remove that
//        // row if that's the case
//        db.removeNegativeEntries();
//        db.saveCurrentSteps(0);
//        db.close();
//        prefs.edit().remove("correctShutdown").apply();

        context.startService(new Intent(context, SensorListener.class));
    }
}
