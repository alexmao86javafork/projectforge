/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.rest.calendar

import org.projectforge.business.calendar.event.model.SeriesModificationMode
import org.projectforge.business.teamcal.admin.TeamCalDao
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.event.CalEventDao
import org.projectforge.business.teamcal.event.model.CalEventDO
import org.projectforge.business.teamcal.externalsubscription.TeamEventExternalSubscriptionCache
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.TimesheetRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTORest
import org.projectforge.rest.dto.CalEvent
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/calEvent")
class CalEventRest() : AbstractDTORest<CalEventDO, CalEvent, CalEventDao>(
        CalEventDao::class.java,
        "plugins.teamcal.event.title") {

    private val log = org.slf4j.LoggerFactory.getLogger(CalEventRest::class.java)

    @Autowired
    private lateinit var teamCalDao: TeamCalDao

    @Autowired
    private lateinit var timesheetRest: TimesheetRest

    @Autowired
    private lateinit var CalendarEventExternalSubscriptionCache: TeamEventExternalSubscriptionCache

    override fun transformForDB(dto: CalEvent): CalEventDO {
        val calendarEventDO = CalEventDO()
        dto.copyTo(calendarEventDO)
        if (dto.selectedSeriesEvent != null) {
            calendarEventDO.setTransientAttribute(CalEventDao.ATTR_SELECTED_ELEMENT, dto.selectedSeriesEvent);
            calendarEventDO.setTransientAttribute(CalEventDao.ATTR_SERIES_MODIFICATION_MODE, dto.seriesModificationMode);
        }
        return calendarEventDO
    }

    override fun transformFromDB(obj: CalEventDO, editMode: Boolean): CalEvent {
        val calendarEvent = CalEvent()
        calendarEvent.copyFrom(obj)
        return calendarEvent
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: CalEvent) {
        if (dto.id != null && dto.hasRecurrence && dto.seriesModificationMode == null) {
            validationErrors.add(ValidationError.create("plugins.teamcal.event.recurrence.change.content"))
            validationErrors.add(ValidationError(fieldId = "seriesModificationMode"))
        }
    }

    /**
     * Params startDate and endDate for creating new events with preset dates.
     * For events of a series, startDate as param selects the event of the series.
     */
    override fun onBeforeGetItemAndLayout(request: HttpServletRequest, dto: CalEvent, userAccess: UILayout.UserAccess) {
        val startDateAsSeconds = NumberHelper.parseLong(request.getParameter("startDate"))
        val endDateSeconds = NumberHelper.parseLong(request.getParameter("endDate"))
        if (dto.id != null) {
            if (startDateAsSeconds != null && endDateSeconds != null && dto.hasRecurrence) {
                // Seems to be a event of a series:
                dto.selectedSeriesEvent = CalEvent(startDate = PFDateTime.from(startDateAsSeconds)!!.sqlTimestamp,
                        endDate = PFDateTime.from(endDateSeconds)!!.sqlTimestamp,
                        allDay = dto.allDay,
                        sequence = dto.sequence)
            }
        } else {
            val calendarId = NumberHelper.parseInteger(request.getParameter("calendar"))
            if (calendarId != null && calendarId > 0) {
                dto.calendar = TeamCalDO()
                dto.calendar?.id = calendarId
            }
        }
        if (startDateAsSeconds != null) dto.startDate = PFDateTime.from(startDateAsSeconds)!!.sqlTimestamp
        if (endDateSeconds != null) dto.endDate = PFDateTime.from(endDateSeconds)!!.sqlTimestamp
    }

    override fun beforeDatabaseAction(request: HttpServletRequest, obj: CalEventDO, dto: CalEvent, operation: OperationType) {
        if (obj.calendar?.id != null) {
            // Calendar from client has only id and title. Get the calendar object from the data base (e. g. owner
            // is needed by the access checker.
            obj.calendar = teamCalDao.getById(obj.calendar.id)
        }
    }

    override fun afterEdit(obj: CalEventDO, dto: CalEvent): ResponseAction {
        return ResponseAction("/calendar")
                .addVariable("date", dto.startDate)
                .addVariable("id", obj.id ?: -1)
    }

    override fun getById(idString: String?, editMode: Boolean, userAccess: UILayout.UserAccess?): CalEvent? {
        if (idString.isNullOrBlank())
            return CalEvent();
        if (idString.contains('-')) { // {calendarId}-{uid}
            val vals = idString.split('-', limit = 2)
            if (vals.size != 2) {
                log.error("Can't get event of subscribed calendar. id must be of form {calId}-{uid} but is '$idString'.")

                return CalEvent()
            }
            try {
                val calId = vals[0].toInt()
                val uid = vals[1]
                val cal = teamCalDao.getById(calId)
                if (cal == null) {
                    log.error("Can't get calendar with id #$calId.")

                    return CalEvent()
                }
                if (!cal.externalSubscription) {
                    log.error("Calendar with id #$calId is not an external subscription, can't get event by uid.")
                    return CalEvent()
                }
                return CalEvent()//return CalendarEventExternalSubscriptionCache.getEvent(calId, uid)
            } catch (ex: NumberFormatException) {
                log.error("Can't get event of subscribed calendar. id must be of form {calId}-{uid} but is '$idString', a NumberFormatException occured.")
                return CalEvent()
            }
        }
        return super.getById(idString, editMode, userAccess)
    }

    /**
     * Will be called by clone button. Sets the id of the form data object to null and deleted to false.
     * @return ResponseAction with [TargetType.UPDATE] and variable "initial" with all the initial data of [getItemAndLayout] as given for new objects.
     */
    @RequestMapping("switch2Timesheet")
    fun switch2Timesheet(request: HttpServletRequest, @RequestBody calendarEvent: CalEvent)
            : ResponseAction {
        return timesheetRest.cloneFromCalendarEvent(request, calendarEvent)
    }

    fun cloneFromTimesheet(request: HttpServletRequest, timesheet: TimesheetDO): ResponseAction {
        val calendarEvent = CalEvent()
        calendarEvent.startDate = timesheet.startTime
        calendarEvent.endDate = timesheet.stopTime
        calendarEvent.location = timesheet.location
        calendarEvent.note = timesheet.description
        val editLayoutData = getItemAndLayout(request, calendarEvent, UILayout.UserAccess(false, true))
        return ResponseAction(url = "/calendar/${getRestPath(RestPaths.EDIT)}", targetType = TargetType.UPDATE)
                .addVariable("data", editLayoutData.data)
                .addVariable("ui", editLayoutData.ui)
                .addVariable("variables", editLayoutData.variables)
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "subject"))
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: CalEvent, userAccess: UILayout.UserAccess): UILayout {
        val calendars = teamCalDao.getAllCalendarsWithFullAccess()
        calendars.removeIf { it.externalSubscription } // Remove full access calendars, but subscribed.
        val calendarSelectValues = calendars.map { it ->
            UISelectValue<Int>(it.id, it.title!!)
        }
        val subject = UIInput("subject", lc)
        subject.focus = true
        val layout = super.createEditLayout(dto, userAccess)
        if (dto.hasRecurrence) {
            val masterEvent = baseDao.getById(dto.id)
            val radioButtonGroup = UIGroup()
            if (masterEvent?.startDate?.before(dto.selectedSeriesEvent?.startDate) ?: true) {
                radioButtonGroup.add(UIRadioButton("seriesModificationMode", SeriesModificationMode.FUTURE, label = "plugins.teamcal.event.recurrence.change.future"))
            } else {
                radioButtonGroup.add(UIRadioButton("seriesModificationMode", SeriesModificationMode.ALL, label = "plugins.teamcal.event.recurrence.change.all"))
            }
            radioButtonGroup.add(UIRadioButton("seriesModificationMode", SeriesModificationMode.SINGLE, label = "plugins.teamcal.event.recurrence.change.single"))
            layout.add(UIFieldset(12, title = "plugins.teamcal.event.recurrence.change.text")
                    .add(radioButtonGroup))
        }
        layout.add(UIFieldset(12)
                .add(UIRow()
                        .add(UICol(6)
                                .add(UISelect<Int>("calendar",
                                        values = calendarSelectValues.toMutableList(),
                                        label = "plugins.teamcal.event.teamCal",
                                        labelProperty = "title",
                                        valueProperty = "id"))
                                .add(subject)
                                .add(lc, "attendees")
                                .add(lc, "location"))
                        .add(UICol(6)
                                .add(lc, "startDate", "endDate", "allDay")
                                .add(lc, "note"))))
                .add(UICustomized("calendar.reminder"))
                .add(UIRow().add(UICol(12).add(UICustomized("calendar.recurrency"))))
        layout.addAction(UIButton("switch",
                title = translate("plugins.teamcal.switchToTimesheetButton"),
                color = UIColor.DARK,
                responseAction = ResponseAction(getRestRootPath("switch2Timesheet"), targetType = TargetType.POST)))
        layout.addTranslations("plugins.teamcal.event.recurrence",
                "plugins.teamcal.event.recurrence.customized",
                "common.recurrence.frequency.yearly",
                "common.recurrence.frequency.monthly",
                "common.recurrence.frequency.weekly",
                "common.recurrence.frequency.daily",
                "common.recurrence.frequency.none",
                "plugins.teamcal.event.reminder",
                "plugins.teamcal.event.reminder.NONE",
                "plugins.teamcal.event.reminder.MESSAGE",
                "plugins.teamcal.event.reminder.MESSAGE_SOUND",
                "plugins.teamcal.event.reminder.MINUTES_BEFORE",
                "plugins.teamcal.event.reminder.HOURS_BEFORE",
                "plugins.teamcal.event.reminder.DAYS_BEFORE")
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}