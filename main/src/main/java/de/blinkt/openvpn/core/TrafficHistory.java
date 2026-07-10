/*
 * Copyright (c) 2012-2017 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashSet;
import java.util.LinkedList;

import static java.lang.Math.max;

import androidx.annotation.NonNull;

/**
 * Created by arne on 23.05.17.
 */

public class TrafficHistory implements Parcelable {

    public static final long PERIODS_TO_KEEP = 5;
    public static final int TIME_PERIOD_MINUTE = 60 * 1000;
    public static final int TIME_PERIOD_HOUR = 3600 * 1000;
    public static final int TIME_PERIOD_DAY = 24 * 3600 * 1000;
    private final LinkedList<TrafficDatapoint> trafficHistorySeconds = new LinkedList<>();
    private final LinkedList<TrafficDatapoint> trafficHistoryMinutes = new LinkedList<>();
    private final LinkedList<TrafficDatapoint> trafficHistoryHours = new LinkedList<>();


    public TrafficHistory() {

    }

    protected TrafficHistory(Parcel in) {
        in.readList(trafficHistorySeconds, getClass().getClassLoader());
        in.readList(trafficHistoryMinutes, getClass().getClassLoader());
        in.readList(trafficHistoryHours, getClass().getClassLoader());
    }

    public static final Creator<TrafficHistory> CREATOR = new Creator<TrafficHistory>() {
        @Override
        public TrafficHistory createFromParcel(Parcel in) {
            return new TrafficHistory(in);
        }

        @Override
        public TrafficHistory[] newArray(int size) {
            return new TrafficHistory[size];
        }
    };

    @NonNull
    public LastDiff getLastDiff(TrafficDatapoint tdp) {

        TrafficDatapoint lasttdp;


        if (trafficHistorySeconds.isEmpty())
            lasttdp = new TrafficDatapoint(0, 0, System.currentTimeMillis());
        else
            lasttdp = trafficHistorySeconds.getLast();

        if (tdp == null) {
            tdp = lasttdp;
            if (trafficHistorySeconds.size() < 2)
                lasttdp = tdp;
            else {
                trafficHistorySeconds.descendingIterator().next();
                tdp = trafficHistorySeconds.descendingIterator().next();
            }
        }

        return new LastDiff(lasttdp, tdp);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(trafficHistorySeconds);
        dest.writeList(trafficHistoryMinutes);
        dest.writeList(trafficHistoryHours);
    }

    @NonNull
    public synchronized LinkedList<TrafficDatapoint> getHours() {
        return new LinkedList<>(trafficHistoryHours);
    }

    @NonNull
    public synchronized LinkedList<TrafficDatapoint> getMinutes() {
        return new LinkedList<>(trafficHistoryMinutes);
    }

    @NonNull
    public synchronized LinkedList<TrafficDatapoint> getSeconds() {
        return new LinkedList<>(trafficHistorySeconds);
    }

    public static LinkedList<TrafficDatapoint> getDummyList() {
        LinkedList<TrafficDatapoint> list = new LinkedList<>();
        list.add(new TrafficDatapoint(0, 0, System.currentTimeMillis()));
        return list;
    }


    public static class TrafficDatapoint implements Parcelable {
        protected TrafficDatapoint(long inBytes, long outBytes, long timestamp) {
            this.in = inBytes;
            this.out = outBytes;
            this.timestamp = timestamp;
        }

        public final long timestamp;
        public final long in;
        public final long out;

        private TrafficDatapoint(Parcel in) {
            timestamp = in.readLong();
            this.in = in.readLong();
            out = in.readLong();
        }

        public static final Creator<TrafficDatapoint> CREATOR = new Creator<TrafficDatapoint>() {
            @Override
            public TrafficDatapoint createFromParcel(Parcel in) {
                return new TrafficDatapoint(in);
            }

            @Override
            public TrafficDatapoint[] newArray(int size) {
                return new TrafficDatapoint[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(timestamp);
            dest.writeLong(in);
            dest.writeLong(out);
        }
    }

    LastDiff add(long in, long out) {
        long time = System.currentTimeMillis();
        return add(in, out, time);
    }

    LastDiff add(long in, long out, long time) {
        TrafficDatapoint tdp = new TrafficDatapoint(in, out, time);

        LastDiff diff = getLastDiff(tdp);
        addDataPoint(tdp);
        return diff;
    }

    private synchronized void addDataPoint(TrafficDatapoint tdp) {
        trafficHistorySeconds.add(tdp);
        removeExcessDataPoints(trafficHistorySeconds, tdp, TIME_PERIOD_MINUTE);


        if (trafficHistoryMinutes.isEmpty() || trafficHistoryMinutes.getLast().timestamp + TIME_PERIOD_MINUTE < tdp.timestamp)
        {
            trafficHistoryMinutes.add(tdp);
            removeExcessDataPoints(trafficHistoryMinutes, tdp, TIME_PERIOD_HOUR);
        }

        if (trafficHistoryHours.isEmpty() || trafficHistoryHours.getLast().timestamp + TIME_PERIOD_HOUR < tdp.timestamp)
        {
            trafficHistoryHours.add(tdp);
            removeExcessDataPoints(trafficHistoryHours, tdp, TIME_PERIOD_DAY);
        }

    }

    private void removeExcessDataPoints(LinkedList<TrafficDatapoint> tpList, TrafficDatapoint newTdp, long timePeriod) {
        HashSet<TrafficDatapoint> toRemove = new HashSet<>();

        // Check if the first and last time point have more than PERIODS_TO_KEEP, so we
        // only hit this condition when we reach a full period more, e.g. reduce seconds
        // from 360 to 300 again
        if ((newTdp.timestamp - tpList.getFirst().timestamp) / timePeriod < (PERIODS_TO_KEEP + 1))
            return;

        for (TrafficDatapoint tph : tpList) {
            // List is iterated from oldest to newest, remember first one that we did not
            if ((newTdp.timestamp - tph.timestamp) / timePeriod >= PERIODS_TO_KEEP)
                toRemove.add(tph);
        }
        tpList.removeAll(toRemove);
    }

    static class LastDiff {

        final private TrafficDatapoint tdp;
        final private TrafficDatapoint lasttdp;

        private LastDiff(TrafficDatapoint lasttdp, TrafficDatapoint tdp) {
            this.lasttdp = lasttdp;
            this.tdp = tdp;
        }

        public long getDiffOut() {
            return max(0, tdp.out - lasttdp.out);
        }

        public long getDiffIn() {
            return max(0, tdp.in - lasttdp.in);
        }

        public long getIn() {
            return tdp.in;
        }

        public long getOut() {
            return tdp.out;
        }

    }


}