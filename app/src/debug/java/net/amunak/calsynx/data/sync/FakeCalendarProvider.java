package net.amunak.calsynx.data.sync;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.CalendarContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FakeCalendarProvider extends ContentProvider {
	public static final String AUTHORITY = "net.amunak.calsynx.test.calendar";

	private static final int EVENTS = 1;
	private static final int EVENT_ID = 2;
	private static final int REMINDERS = 3;
	private static final int REMINDER_ID = 4;
	private static final int ATTENDEES = 5;
	private static final int ATTENDEE_ID = 6;

	private static final String[] DEFAULT_COLUMNS = new String[] {
		CalendarContract.Events._ID,
		CalendarContract.Events.CALENDAR_ID,
		CalendarContract.Events.TITLE,
		CalendarContract.Events.DTSTART,
		CalendarContract.Events.DTEND,
		CalendarContract.Events.DURATION,
		CalendarContract.Events.ALL_DAY,
		CalendarContract.Events.EVENT_TIMEZONE,
		CalendarContract.Events.EVENT_END_TIMEZONE,
		CalendarContract.Events.RRULE,
		CalendarContract.Events.EXDATE,
		CalendarContract.Events.EXRULE,
		CalendarContract.Events.RDATE,
		CalendarContract.Events.ORIGINAL_ID,
		CalendarContract.Events.ORIGINAL_INSTANCE_TIME,
		CalendarContract.Events.ORIGINAL_ALL_DAY,
		CalendarContract.Events.STATUS,
		CalendarContract.Events.EVENT_LOCATION,
		CalendarContract.Events.DESCRIPTION,
		CalendarContract.Events.AVAILABILITY,
		CalendarContract.Events.ACCESS_LEVEL,
		CalendarContract.Events.EVENT_COLOR,
		CalendarContract.Events.ORGANIZER,
		CalendarContract.Events.OWNER_ACCOUNT,
		CalendarContract.Events.DELETED
	};

	private static final String[] REMINDER_COLUMNS = new String[] {
		CalendarContract.Reminders._ID,
		CalendarContract.Reminders.EVENT_ID,
		CalendarContract.Reminders.MINUTES,
		CalendarContract.Reminders.METHOD
	};

	private static final String[] ATTENDEE_COLUMNS = new String[] {
		CalendarContract.Attendees._ID,
		CalendarContract.Attendees.EVENT_ID,
		CalendarContract.Attendees.ATTENDEE_NAME,
		CalendarContract.Attendees.ATTENDEE_EMAIL,
		CalendarContract.Attendees.ATTENDEE_TYPE,
		CalendarContract.Attendees.ATTENDEE_RELATIONSHIP,
		CalendarContract.Attendees.ATTENDEE_STATUS,
		CalendarContract.Attendees.ATTENDEE_IDENTITY,
		CalendarContract.Attendees.ATTENDEE_ID_NAMESPACE
	};

	private final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
	private final Map<Long, ContentValues> events = new LinkedHashMap<>();
	private final Map<Long, ContentValues> reminders = new LinkedHashMap<>();
	private final Map<Long, ContentValues> attendees = new LinkedHashMap<>();
	private long nextId = 1L;
	private long nextReminderId = 1L;
	private long nextAttendeeId = 1L;

	@Override
	public boolean onCreate() {
		matcher.addURI(AUTHORITY, "events", EVENTS);
		matcher.addURI(AUTHORITY, "events/#", EVENT_ID);
		matcher.addURI(AUTHORITY, "reminders", REMINDERS);
		matcher.addURI(AUTHORITY, "reminders/#", REMINDER_ID);
		matcher.addURI(AUTHORITY, "attendees", ATTENDEES);
		matcher.addURI(AUTHORITY, "attendees/#", ATTENDEE_ID);
		return true;
	}

	@Override
	public Cursor query(
		Uri uri,
		String[] projection,
		String selection,
		String[] selectionArgs,
		String sortOrder
	) {
		int match = matcher.match(uri);
		if (match == EVENTS) {
			String[] columns = projection != null ? projection : DEFAULT_COLUMNS;
			MatrixCursor cursor = new MatrixCursor(columns);
			List<ContentValues> rows = new ArrayList<>(events.values());
			rows.removeIf(row -> !matches(row, selection, selectionArgs));
			Collections.sort(rows, Comparator.comparingLong(row ->
				valueLong(row, CalendarContract.Events.DTSTART, 0L)
			));
			for (ContentValues row : rows) {
				Object[] out = new Object[columns.length];
				for (int i = 0; i < columns.length; i++) {
					out[i] = row.get(columns[i]);
				}
				cursor.addRow(out);
			}
			return cursor;
		}
		if (match == REMINDERS) {
			String[] columns = projection != null ? projection : REMINDER_COLUMNS;
			MatrixCursor cursor = new MatrixCursor(columns);
			List<ContentValues> rows = new ArrayList<>(reminders.values());
			rows.removeIf(row -> !matchesEventId(row, selection, selectionArgs));
			for (ContentValues row : rows) {
				Object[] out = new Object[columns.length];
				for (int i = 0; i < columns.length; i++) {
					out[i] = row.get(columns[i]);
				}
				cursor.addRow(out);
			}
			return cursor;
		}
		if (match == ATTENDEES) {
			String[] columns = projection != null ? projection : ATTENDEE_COLUMNS;
			MatrixCursor cursor = new MatrixCursor(columns);
			List<ContentValues> rows = new ArrayList<>(attendees.values());
			rows.removeIf(row -> !matchesEventId(row, selection, selectionArgs));
			for (ContentValues row : rows) {
				Object[] out = new Object[columns.length];
				for (int i = 0; i < columns.length; i++) {
					out[i] = row.get(columns[i]);
				}
				cursor.addRow(out);
			}
			return cursor;
		}
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int match = matcher.match(uri);
		if (values == null) {
			return null;
		}
		if (match == EVENTS) {
			long id = values.containsKey(CalendarContract.Events._ID)
				? values.getAsLong(CalendarContract.Events._ID)
				: nextId++;
			ContentValues record = new ContentValues(values);
			record.put(CalendarContract.Events._ID, id);
			if (!record.containsKey(CalendarContract.Events.DELETED)) {
				record.put(CalendarContract.Events.DELETED, 0);
			}
			events.put(id, record);
			return ContentUris.withAppendedId(uri, id);
		}
		if (match == REMINDERS) {
			long id = nextReminderId++;
			ContentValues record = new ContentValues(values);
			record.put(CalendarContract.Reminders._ID, id);
			reminders.put(id, record);
			return ContentUris.withAppendedId(uri, id);
		}
		if (match == ATTENDEES) {
			long id = nextAttendeeId++;
			ContentValues record = new ContentValues(values);
			record.put(CalendarContract.Attendees._ID, id);
			attendees.put(id, record);
			return ContentUris.withAppendedId(uri, id);
		}
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int match = matcher.match(uri);
		if (values == null) {
			return 0;
		}
		if (match == EVENT_ID) {
			long id = ContentUris.parseId(uri);
			ContentValues record = events.get(id);
			if (record == null) {
				return 0;
			}
			applyUpdate(record, values);
			return 1;
		}
		if (match == REMINDER_ID) {
			long id = ContentUris.parseId(uri);
			ContentValues record = reminders.get(id);
			if (record == null) {
				return 0;
			}
			applyUpdate(record, values);
			return 1;
		}
		if (match == ATTENDEE_ID) {
			long id = ContentUris.parseId(uri);
			ContentValues record = attendees.get(id);
			if (record == null) {
				return 0;
			}
			applyUpdate(record, values);
			return 1;
		}
		return 0;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int match = matcher.match(uri);
		if (match == EVENT_ID) {
			long id = ContentUris.parseId(uri);
			return events.remove(id) != null ? 1 : 0;
		}
		if (match == EVENTS) {
			int count = events.size();
			events.clear();
			return count;
		}
		if (match == REMINDER_ID) {
			long id = ContentUris.parseId(uri);
			return reminders.remove(id) != null ? 1 : 0;
		}
		if (match == REMINDERS) {
			return deleteByEventId(reminders, selection, selectionArgs);
		}
		if (match == ATTENDEE_ID) {
			long id = ContentUris.parseId(uri);
			return attendees.remove(id) != null ? 1 : 0;
		}
		if (match == ATTENDEES) {
			return deleteByEventId(attendees, selection, selectionArgs);
		}
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return "vnd.android.cursor.item/event";
	}

	private void applyUpdate(ContentValues record, ContentValues values) {
		for (String key : values.keySet()) {
			Object value = values.get(key);
			if (value == null) {
				record.putNull(key);
			} else if (value instanceof String) {
				record.put(key, (String) value);
			} else if (value instanceof Long) {
				record.put(key, (Long) value);
			} else if (value instanceof Integer) {
				record.put(key, (Integer) value);
			} else if (value instanceof Boolean) {
				record.put(key, ((Boolean) value) ? 1 : 0);
			} else {
				record.put(key, value.toString());
			}
		}
	}

	private int deleteByEventId(Map<Long, ContentValues> table, String selection, String[] selectionArgs) {
		if (selection == null || selectionArgs == null || selectionArgs.length == 0) {
			int count = table.size();
			table.clear();
			return count;
		}
		long eventId = toLong(selectionArgs[0], -1L);
		int count = 0;
		List<Long> toRemove = new ArrayList<>();
		for (Map.Entry<Long, ContentValues> entry : table.entrySet()) {
			Long value = entry.getValue().getAsLong(CalendarContract.Reminders.EVENT_ID);
			if (value != null && value == eventId) {
				toRemove.add(entry.getKey());
			}
		}
		for (Long key : toRemove) {
			table.remove(key);
			count += 1;
		}
		return count;
	}

	private boolean matches(ContentValues row, String selection, String[] selectionArgs) {
		if (selection == null) {
			return true;
		}
		if (selection.contains(CalendarContract.Events._ID + " IN")) {
			if (selectionArgs == null) {
				return false;
			}
			long id = valueLong(row, CalendarContract.Events._ID, -1L);
			for (String arg : selectionArgs) {
				if (id == toLong(arg, -2L)) {
					return true;
				}
			}
			return false;
		}
		if (selection.contains(CalendarContract.Events.CALENDAR_ID + " = ?")) {
			if (selectionArgs == null || selectionArgs.length == 0) {
				return false;
			}
			long expected = toLong(selectionArgs[0], -1L);
			if (valueLong(row, CalendarContract.Events.CALENDAR_ID, -1L) != expected) {
				return false;
			}
		}
		if (selection.contains(CalendarContract.Events.DELETED + " = 0")) {
			if (valueInt(row, CalendarContract.Events.DELETED, 0) != 0) {
				return false;
			}
		}
		if (selection.contains(CalendarContract.Events.RRULE + " IS NOT NULL")) {
			String rrule = row.getAsString(CalendarContract.Events.RRULE);
			if (rrule != null && !rrule.isEmpty()) {
				return true;
			}
			if (selectionArgs == null || selectionArgs.length < 3) {
				return false;
			}
			long end = toLong(selectionArgs[1], Long.MAX_VALUE);
			long start = toLong(selectionArgs[2], Long.MIN_VALUE);
			long dtStart = valueLong(row, CalendarContract.Events.DTSTART, Long.MAX_VALUE);
			Long dtEnd = row.getAsLong(CalendarContract.Events.DTEND);
			return dtStart <= end && (dtEnd == null || dtEnd >= start);
		}
		return true;
	}

	private boolean matchesEventId(ContentValues row, String selection, String[] selectionArgs) {
		if (selection == null) {
			return true;
		}
		if (selectionArgs == null || selectionArgs.length == 0) {
			return false;
		}
		if (selection.contains("event_id")) {
			long expected = toLong(selectionArgs[0], -1L);
			Long actual = row.getAsLong(CalendarContract.Reminders.EVENT_ID);
			return actual != null && actual == expected;
		}
		return true;
	}

	private static long valueLong(ContentValues row, String key, long fallback) {
		Long value = row.getAsLong(key);
		return value != null ? value : fallback;
	}

	private static int valueInt(ContentValues row, String key, int fallback) {
		Integer value = row.getAsInteger(key);
		return value != null ? value : fallback;
	}

	private static long toLong(String raw, long fallback) {
		try {
			return Long.parseLong(raw);
		} catch (NumberFormatException ex) {
			return fallback;
		}
	}
}
