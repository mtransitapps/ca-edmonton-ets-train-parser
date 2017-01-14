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
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
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
// https://data.edmonton.ca/Transit/ETS-Bus-Schedule-GTFS-Data-Feed-zipped-files/gzhc-5ss6
// https://data.edmonton.ca/download/gzhc-5ss6/application/zip
// http://www.edmonton.ca/ets/ets-data-for-developers.aspx
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
		System.out.printf("\nGenerating ETS train data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		System.out.printf("\nGenerating ETS train data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		return gRoute.getRouteType() != MAgency.ROUTE_TYPE_LIGHT_RAIL; // declared as light rail but we classify it as a train (not on the road)
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		return Long.parseLong(gRoute.getRouteId()); // using route short name as route ID
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		return String.valueOf(gRoute.getRouteId());
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
		System.out.printf("\nUnexpected route color for %s!\n", gRoute);
		System.exit(-1);
		return null;
	}

	private static final long RID_CAPITAL_LINE = 501L;
	private static final long RID_METRO_LINE = 502L;

	private static final String CENTURY_PK = "Century Pk";
	private static final String CLAREVIEW = "Clareview";
	private static final String NAIT = "NAIT";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(RID_CAPITAL_LINE, new RouteTripSpec(RID_CAPITAL_LINE, //
				0, MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				1, MTrip.HEADSIGN_TYPE_STRING, CLAREVIEW) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"7977", "1889", "1876", "1891", "2316", "2115", "4982" //
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"4982", "2116", "2969", "1926", "1691", "1742", "7977" //
						})) //
				.compileBothTripSort());
		map2.put(RID_METRO_LINE, new RouteTripSpec(RID_METRO_LINE, //
				0, MTrip.HEADSIGN_TYPE_STRING, CENTURY_PK, //
				1, MTrip.HEADSIGN_TYPE_STRING, NAIT) //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"1116", "1118", //
								"1876", //
								"2019", //
								/* + */"2005"/* + */, //
								/* + */"9981"/* + */, //
								"2113", "4982" //
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"4982", "2114",
						/* + */"2116"/* + */, //
								/* + */"2005"/* + */, //
								"2014", "1117", "1116" //
						})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		System.out.printf("\fUnexpected split trip (unexpected route ID: %s) %s", mRoute.getId(), gTrip);
		System.exit(-1);
		return null;
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()));
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		System.out.printf("\n%s: Unexpected trip %s.\n", mRoute.getId(), gTrip);
		System.exit(-1);
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
