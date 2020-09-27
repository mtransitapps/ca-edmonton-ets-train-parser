package org.mtransit.parser.ca_edmonton_ets_train;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GIDs;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

// https://data.edmonton.ca/
// http://www.edmonton.ca/ets/ets-data-for-developers.aspx
// https://data.edmonton.ca/Transit/ETS-Bus-Schedule-GTFS-Data-Schedules-zipped-files/urjq-fvmq
// https://gtfs.edmonton.ca/TMGTFSRealTimeWebService/GTFS/GTFS.zip
public class EdmontonETSTrainAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-edmonton-ets-train-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new EdmontonETSTrainAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		MTLog.log("Generating ETS train data...");
		long start = System.currentTimeMillis();
		boolean isNext = "next_".equalsIgnoreCase(args[2]);
		if (isNext) {
			setupNext();
		}
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		MTLog.log("Generating ETS train data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	private void setupNext() {
		// DO NOTHING
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_TRAIN;
	}

	private static final int AGENCY_ID_INT = GIDs.getInt("1"); // Edmonton Transit Service ONLY

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (gRoute.isDifferentAgency(AGENCY_ID_INT)) {
			return true; // exclude
		}
		return gRoute.getRouteType() != MAgency.ROUTE_TYPE_LIGHT_RAIL; // declared as light rail but we classify it as a train (not on the road)
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		return Long.parseLong(getRouteShortName(gRoute)); // using route short name as route ID
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			return gRoute.getRouteShortName();
		}
		if (Utils.isDigitsOnly(gRoute.getRouteId())) {
			return gRoute.getRouteId();
		}
		throw new MTLog.Fatal("Unexpected route ID for %s!", gRoute);
	}

	private static final String RSN_CAPITAL_LINE = "Capital";
	private static final String RSN_METRO_LINE = "Metro";

	private static final Pattern CLEAN_STARTS_LRT = Pattern.compile("(^lrt )", Pattern.CASE_INSENSITIVE);

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String gRouteLongName = gRoute.getRouteLongName();
		gRouteLongName = CLEAN_STARTS_LRT.matcher(gRouteLongName).replaceAll(StringUtils.EMPTY);
		return CleanUtils.cleanLabel(gRouteLongName);
	}

	private static final String AGENCY_COLOR_BLUE = "2D3092"; // BLUE (from Wikipedia SVG)

	private static final String AGENCY_COLOR = AGENCY_COLOR_BLUE;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String COLOR_CAPITAL_LINE = "0D4BA0"; // BLUE (from PDF map)
	private static final String COLOR_METRO_LINE = "EE2D24"; // RED (from PDF map)

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
				int rsn = Integer.parseInt(gRoute.getRouteShortName());
				switch (rsn) {
				case 501:
					return COLOR_CAPITAL_LINE;
				case 502:
					return COLOR_METRO_LINE;
				}
			}
			if (RSN_CAPITAL_LINE.equalsIgnoreCase(gRoute.getRouteShortName())) {
				return COLOR_CAPITAL_LINE;
			} else if (RSN_METRO_LINE.equalsIgnoreCase(gRoute.getRouteShortName())) {
				return COLOR_METRO_LINE;
			}
			throw new MTLog.Fatal("Unexpected route color for %s!", gRoute);
		}
		return super.getRouteColor(gRoute);
	}

	private static final long RID_CAPITAL_LINE = 501L;
	private static final long RID_METRO_LINE = 502L;

	private static final String CENTURY_PK = "Century Pk";
	private static final String NAIT = "NAIT";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == RID_CAPITAL_LINE) {
			if (Arrays.asList( //
					"South Campus", //
					CENTURY_PK //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CENTURY_PK, mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == RID_METRO_LINE) {
			if (Arrays.asList( //
					"Downtown", // <>
					"Churchill", //
					"Health Sciences", //
					"South Campus", //
					CENTURY_PK //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(CENTURY_PK, mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Downtown", // <>
					NAIT //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(NAIT, mTrip.getHeadsignId());
				return true;
			}
		}
		throw new MTLog.Fatal("Unexpected trips to merge: %s & %s!", mTrip, mTripToMerge);
	}

	private static final Pattern N_A_I_T_ = Pattern.compile("(n a i t)", Pattern.CASE_INSENSITIVE);
	private static final String N_A_I_T_REPLACEMENT = NAIT;

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = N_A_I_T_.matcher(tripHeadsign).replaceAll(N_A_I_T_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern ENDS_WITH_STATION = Pattern.compile("([\\s]*station[\\s]*$)", Pattern.CASE_INSENSITIVE);

	private static final String EDMONTON = "Edm";
	private static final Pattern EDMONTON_ = Pattern.compile("((^|\\W){1}(edmonton)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String EDMONTON_REPLACEMENT = "$2" + EDMONTON + "$4";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = ENDS_WITH_STATION.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = EDMONTON_.matcher(gStopName).replaceAll(EDMONTON_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
