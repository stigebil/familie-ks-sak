package no.nav.familie.ks.sak.common.util

import java.sql.Date
import java.time.YearMonth
import javax.persistence.AttributeConverter

class YearMonthConverter : AttributeConverter<YearMonth, Date> {

    override fun convertToDatabaseColumn(yearMonth: YearMonth?): Date? = yearMonth?.let {
        Date.valueOf(it.toLocalDate())
    }

    override fun convertToEntityAttribute(date: Date?): YearMonth? = date?.toLocalDate()?.tilYearMonth()
}
