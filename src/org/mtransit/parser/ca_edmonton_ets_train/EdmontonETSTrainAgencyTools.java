package org.mtransit.parser.ca_edmonton_ets_train;

import java.util.HashSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;

// http://www.edmonton.ca/transportation/ets/about_ets/ets-data-for-developers.aspx
// http://webdocs.edmonton.ca/transit/etsdatafeed/google_transit.zip
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
		System.out.printf("Generating ETS train data...\n");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("Generating ETS train data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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
		return gRoute.route_type != MAgency.ROUTE_TYPE_LIGHT_RAIL; // declared as light rail but we classify it as a train (not on the road)
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		return Long.parseLong(gRoute.route_short_name); // using route short name as route ID
	}

	private static final int RSN_CAPITAL_LINE = 501;

	private static final Pattern CLEAN_STARTS_LRT = Pattern.compile("(^lrt )", Pattern.CASE_INSENSITIVE);

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String gRouteLongName = gRoute.route_long_name;
		gRouteLongName = CLEAN_STARTS_LRT.matcher(gRouteLongName).replaceAll(StringUtils.EMPTY);
		return MSpec.cleanLabel(gRouteLongName);
	}

	private static final String AGENCY_COLOR_BLUE = "2D3092"; // BLUE (from Wikipedia SVG)

	private static final String AGENCY_COLOR = AGENCY_COLOR_BLUE;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String COLOR_CAPITAL_LINE = "0D4BA0"; // BLUE (from PDF map)

	@Override
	public String getRouteColor(GRoute gRoute) {
		int rsn = Integer.parseInt(gRoute.route_short_name);
		switch (rsn) {
		// @formatter:off
		case RSN_CAPITAL_LINE: return COLOR_CAPITAL_LINE;
		// @formatter:on
		default:
			System.out.println("Unexpected route color " + gRoute);
			System.exit(-1);
			return null;
		}
	}

	private static final long RID_CAPITAL_LINE = 501l;

	@Override
	public HashSet<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (mRoute.id == RID_CAPITAL_LINE) {
			HashSet<MTrip> mTrips = new HashSet<MTrip>();
			MTrip mTripCenturyPk = new MTrip(mRoute.id);
			mTripCenturyPk.setHeadsignString(CENTURY_PK, CAPITAL_LINE_DIRECTION_ID_CENTURY_PK);
			mTrips.add(mTripCenturyPk);
			MTrip mTripClareview = new MTrip(mRoute.id);
			mTripClareview.setHeadsignString(CLAREVIEW, CAPITAL_LINE_DIRECTION_ID_CLAREVIEW);
			mTrips.add(mTripClareview);
			return mTrips;
		}
		System.out.println("Unexpected split trip (unexpected route ID: " + mRoute.id + ") " + gTrip);
		System.exit(-1);
		return null;
	}

	private static final String CENTURY_PK_STOP_ID = "4982";
	private static final String CLAREVIEW_STOP_ID = "7977";

	private static final int CAPITAL_LINE_DIRECTION_ID_CENTURY_PK = 0;
	private static final int CAPITAL_LINE_DIRECTION_ID_CLAREVIEW = 1;

	private static final String CENTURY_PK = "Century Pk";
	private static final String CLAREVIEW = "Clareview";

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (mRoute.id == RID_CAPITAL_LINE) {
			return;
		}
		System.out.println("Unexpected trip (unexpected route ID: " + mRoute.id + ") " + gTrip);
		System.exit(-1);
	}

	private static final HashSet<String> CAPITAL_LINE_STOP_IDS_CENTURY_PK;
	private static final HashSet<String> CAPITAL_LINE_STOP_IDS_CLAREVIEW;
	static {
		HashSet<String> set = new HashSet<String>();
		set.add("2114");
		set.add("2116");
		set.add("9982");
		set.add("2014");
		set.add("2969");
		set.add("1754");
		set.add("1926");
		set.add("1985");
		set.add("1863");
		set.add("1691");
		set.add("1981");
		set.add("1742");
		set.add("7830");
		CAPITAL_LINE_STOP_IDS_CENTURY_PK = set;
		set = new HashSet<String>();
		set.add("7692");
		set.add("1889");
		set.add("1723");
		set.add("1876");
		set.add("1935");
		set.add("1774");
		set.add("1891");
		set.add("1925");
		set.add("2316");
		set.add("2019");
		set.add("9981");
		set.add("2115");
		set.add("2113");
		CAPITAL_LINE_STOP_IDS_CLAREVIEW = set;
	}

	private static final long TID_CAPITAL_LINE_CLAREVIEW = MTrip.getNewId(RID_CAPITAL_LINE, CAPITAL_LINE_DIRECTION_ID_CLAREVIEW);
	private static final long TID_CAPITAL_LINE_CENTURY_PK = MTrip.getNewId(RID_CAPITAL_LINE, CAPITAL_LINE_DIRECTION_ID_CENTURY_PK);

	private static final int STOP_SEQUENCE_FIRST = 1;
	private static final int STOP_SEQUENCE_LAST = Integer.MAX_VALUE;
	private static final Integer[] STOP_SEQUENCES_FIRST = new Integer[] { STOP_SEQUENCE_FIRST };
	private static final Integer[] STOP_SEQUENCES_FIRST_AND_LAST = new Integer[] { STOP_SEQUENCE_FIRST, STOP_SEQUENCE_LAST };

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, HashSet<MTrip> splitTrips, GSpec gtfs) {
		if (mRoute.id == RID_CAPITAL_LINE) {
			if (CENTURY_PK_STOP_ID.equals(gTripStop.getStopId())) {
				if (gTripStop.getStopSequence() == 1) {
					return new Pair<Long[], Integer[]>(new Long[] { TID_CAPITAL_LINE_CLAREVIEW }, STOP_SEQUENCES_FIRST);
				} else {
					if (isLastTripStop(gTrip, gTripStop, gtfs)) {
						return new Pair<Long[], Integer[]>(new Long[] { TID_CAPITAL_LINE_CENTURY_PK }, new Integer[] { gTripStop.getStopSequence() });
					} else {
						return new Pair<Long[], Integer[]>(new Long[] { //
								TID_CAPITAL_LINE_CLAREVIEW, TID_CAPITAL_LINE_CENTURY_PK }, //
								STOP_SEQUENCES_FIRST_AND_LAST);
					}
				}
			}
			if (CLAREVIEW_STOP_ID.equals(gTripStop.getStopId())) {
				if (gTripStop.getStopSequence() == 1) {
					return new Pair<Long[], Integer[]>(new Long[] { TID_CAPITAL_LINE_CENTURY_PK }, STOP_SEQUENCES_FIRST);
				} else {
					if (isLastTripStop(gTrip, gTripStop, gtfs)) {
						return new Pair<Long[], Integer[]>(new Long[] { TID_CAPITAL_LINE_CLAREVIEW }, new Integer[] { gTripStop.getStopSequence() });
					} else {
						return new Pair<Long[], Integer[]>(new Long[] { //
								TID_CAPITAL_LINE_CENTURY_PK, TID_CAPITAL_LINE_CLAREVIEW }, //
								STOP_SEQUENCES_FIRST_AND_LAST);
					}
				}
			}
			if (CAPITAL_LINE_STOP_IDS_CENTURY_PK.contains(gTripStop.getStopId())) {
				return new Pair<Long[], Integer[]>(new Long[] { TID_CAPITAL_LINE_CENTURY_PK }, new Integer[] { gTripStop.getStopSequence() });
			} else if (CAPITAL_LINE_STOP_IDS_CLAREVIEW.contains(gTripStop.getStopId())) {
				return new Pair<Long[], Integer[]>(new Long[] { TID_CAPITAL_LINE_CLAREVIEW }, new Integer[] { gTripStop.getStopSequence() });
			} else {
				System.out.println("Unexptected trip stop to split " + gTripStop);
				System.exit(-1);
			}
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, gtfs);
	}

	private boolean isLastTripStop(GTrip gTrip, GTripStop gTripStop, GSpec gtfs) {
		for (GTripStop ts : gtfs.getTripStops(gTrip.getTripId())) {
			if (!ts.getTripId().equals(gTrip.getTripId())) {
				continue;
			}
			if (ts.getStopSequence() > gTripStop.getStopSequence()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		return MSpec.cleanLabel(tripHeadsign);
	}

	private static final Pattern ENDS_WITH_STATION = Pattern.compile("([\\s]*station[\\s]*$)", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = ENDS_WITH_STATION.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = MSpec.cleanStreetTypes(gStopName);
		gStopName = MSpec.cleanNumbers(gStopName);
		return MSpec.cleanLabel(gStopName);
	}
}
