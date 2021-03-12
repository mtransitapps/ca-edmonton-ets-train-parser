package org.mtransit.parser.ca_edmonton_ets_train;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.StringUtils;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GIDs;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

import java.util.HashSet;
import java.util.regex.Pattern;

import static org.mtransit.parser.Constants.EMPTY;

// https://data.edmonton.ca/
// http://www.edmonton.ca/ets/ets-data-for-developers.aspx
// https://data.edmonton.ca/Transit/ETS-Bus-Schedule-GTFS-Data-Schedules-zipped-files/urjq-fvmq
// https://gtfs.edmonton.ca/TMGTFSRealTimeWebService/GTFS/GTFS.zip
public class EdmontonETSTrainAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new EdmontonETSTrainAgencyTools().start(args);
	}

	@NotNull
	public String getAgencyName() {
		return "ETS";
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_TRAIN;
	}

	private static final int AGENCY_ID_INT = GIDs.getInt("1"); // Edmonton Transit Service ONLY

	@Override
	public boolean excludeRoute(@NotNull GRoute gRoute) {
		if (gRoute.isDifferentAgency(AGENCY_ID_INT)) {
			return true; // exclude
		}
		return gRoute.getRouteType() != MAgency.ROUTE_TYPE_LIGHT_RAIL; // declared as light rail but we classify it as a train (not on the road)
	}

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		if (RSN_CAPITAL_LINE.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return 501L;
		} else if (RSN_METRO_LINE.equalsIgnoreCase(gRoute.getRouteShortName())) {
			return 502L;
		}
		return Long.parseLong(gRoute.getRouteShortName()); // using route short name as route ID
	}

	private static final String RSN_CAPITAL_LINE = "Capital";
	private static final String RSN_METRO_LINE = "Metro";

	private static final Pattern CLEAN_STARTS_LRT = Pattern.compile("(^lrt )", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		String gRouteLongName = gRoute.getRouteLongNameOrDefault();
		gRouteLongName = CLEAN_STARTS_LRT.matcher(gRouteLongName).replaceAll(EMPTY);
		return CleanUtils.cleanLabel(gRouteLongName);
	}

	private static final String AGENCY_COLOR_BLUE = "2D3092"; // BLUE (from Wikipedia SVG)

	private static final String AGENCY_COLOR = AGENCY_COLOR_BLUE;

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String COLOR_CAPITAL_LINE = "0D4BA0"; // BLUE (from PDF map)
	private static final String COLOR_METRO_LINE = "EE2D24"; // RED (from PDF map)

	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute) {
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

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final String NAIT = "NAIT";
	private static final Pattern N_A_I_T_ = Pattern.compile("(n a i t)", Pattern.CASE_INSENSITIVE);
	private static final String N_A_I_T_REPLACEMENT = NAIT;

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = N_A_I_T_.matcher(tripHeadsign).replaceAll(N_A_I_T_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanBounds(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern ENDS_WITH_STATION = Pattern.compile("([\\s]*station[\\s]*$)", Pattern.CASE_INSENSITIVE);

	private static final String EDMONTON = "Edm";
	private static final Pattern EDMONTON_ = Pattern.compile("((^|\\W)(edmonton)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String EDMONTON_REPLACEMENT = "$2" + EDMONTON + "$4";

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = ENDS_WITH_STATION.matcher(gStopName).replaceAll(EMPTY);
		gStopName = EDMONTON_.matcher(gStopName).replaceAll(EDMONTON_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
