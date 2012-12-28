package org.brekka.commons.tapestry.support;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;

public class ElapsedPeriodFormat extends Format {
    /**
     * Serial UID
     */
    private static final long serialVersionUID = -2276827813914996073L;

    private final FastDateFormat lessThanWeek;
    private final FastDateFormat lessThanYear;
    private final FastDateFormat moreThanYear;
    
    private final Messages messages;
    
    protected ElapsedPeriodFormat(Locale locale, Messages messages, String lessThanWeekFormat, 
            String lessThanYearFormat, String moreThanYearFormat) {
        this.messages = messages;
        this.lessThanWeek = FastDateFormat.getInstance(lessThanWeekFormat, locale);
        this.lessThanYear = FastDateFormat.getInstance(lessThanYearFormat, locale);
        this.moreThanYear = FastDateFormat.getInstance(moreThanYearFormat, locale);
    }
    
    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo,
            FieldPosition pos) {
        Date date = (Date) obj;
        long now = System.currentTimeMillis();
        long diff = (now - date.getTime());
        String t;
        if (diff < DateUtils.MILLIS_PER_MINUTE) {
            t = messages.get("ElapsedPeriodFormat.very-recently");
        } else if (diff < DateUtils.MILLIS_PER_HOUR) {
            int minutes = (int) (diff / DateUtils.MILLIS_PER_MINUTE);
            if (minutes == 1) {
                t = messages.get("ElapsedPeriodFormat.one-minute-ago");
            } else {
                t = messages.format("ElapsedPeriodFormat.x-minutes-ago", minutes);
            }
        } else if (diff < DateUtils.MILLIS_PER_DAY) {
            int hours = (int) (diff / DateUtils.MILLIS_PER_HOUR);
            if (hours == 1) {
                t = messages.get("ElapsedPeriodFormat.one-hour-ago");
            } else {
                t = messages.format("ElapsedPeriodFormat.x-hours-ago", hours);
            }
        } else if (diff < (DateUtils.MILLIS_PER_DAY * 7)) {
            t= lessThanWeek.format(date);
        } else if (diff < (DateUtils.MILLIS_PER_DAY * 365)) {
            t= lessThanYear.format(date);
        } else {
            t = moreThanYear.format(date);
        }
        return toAppendTo.append(t);
    }

    @Override
    public Object parseObject(String source, ParsePosition pos) {
        throw new UnsupportedOperationException("Parsing is not supported");
    }
    
    public static final ElapsedPeriodFormat getDateTimeInstance(Locale locale, Messages messages) {
        return new ElapsedPeriodFormat(locale, messages, "EEEEE, HH:mm", "MMMMM dd, HH:mm", "dd/MM/yyyy HH:mm");
    }
    
    public static final ElapsedPeriodFormat getDateInstance(Locale locale, Messages messages) {
        return new ElapsedPeriodFormat(locale, messages, "EEEEE", "MMMMM dd", "dd/MM/yyyy");
    }
}
