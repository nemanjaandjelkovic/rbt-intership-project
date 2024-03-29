package rs.rbt.internship.employee.service

import org.apache.commons.validator.routines.DateValidator
import org.apache.commons.validator.routines.EmailValidator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import rs.rbt.internship.database.model.Employee
import rs.rbt.internship.database.model.UsedVacation
import rs.rbt.internship.database.model.VacationDayPerYear
import rs.rbt.internship.database.service.EmployeeService
import rs.rbt.internship.database.service.UsedVacationService
import rs.rbt.internship.database.service.VacationDayPerYearService
import rs.rbt.internship.employee.exception.MessageError
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class EmployeeBusinessService {
    companion object{
        private const val dateFormat:String = "d/M/yyyy"
    }

    @Autowired
    lateinit var employeeService: EmployeeService

    @Autowired
    lateinit var usedVacationService: UsedVacationService

    var usedVacationDaysService: UsedVacationDaysService = UsedVacationDaysService()

    @Autowired
    lateinit var vacationDayPerYearService: VacationDayPerYearService



    fun showListRecordsOfUsedVacation(
        dateStart: String,
        dateEnd: String,
        employeeEmail: String
    ): MutableList<UsedVacation> {


        var usedVacations: MutableList<UsedVacation> = mutableListOf()
        val dateStartEnd: MutableList<LocalDate> = convertParameters(dateStart, dateEnd)

        if (parametersValid(dateStart, dateEnd, employeeEmail)) {
            if (dateStart<dateEnd)
            {
                val employee = employeeService.findEmployeeByEmail(employeeEmail)
                usedVacations = usedVacationService.dates(dateStartEnd[0], dateStartEnd[1], employee.id)
            }
            else{
                throw ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, MessageError.WrongDate.message
                )
            }
        } else {

        }
        return usedVacations
    }

    fun parametersValid(
        dateStart: String,
        dateEnd: String,
        employeeEmail: String
    ): Boolean {
        val emailValidated: Boolean = EmailValidator.getInstance().isValid(employeeEmail)
        val dateStartValidated: Boolean = DateValidator.getInstance().isValid(dateStart, dateFormat)
        val dateEndValidated: Boolean = DateValidator.getInstance().isValid(dateEnd, dateFormat)
        return if (employeeService.employeeExists(employeeEmail)) {
            emailValidated && dateEndValidated && dateStartValidated
        } else {
            false
        }
    }

    fun convertParameters(dateStart: String, dateEnd: String): MutableList<LocalDate> {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat)
        val dates: MutableList<LocalDate> = mutableListOf()
        dates.add(LocalDate.parse(dateStart, formatter))
        dates.add(LocalDate.parse(dateEnd, formatter))

        return dates
    }


    fun addVacation(dateStart: String, dateEnd: String, employeeEmail: String) {
        println(parametersValid(dateStart, dateEnd, employeeEmail))
        println("$dateStart $dateEnd")
        if (parametersValid(dateStart, dateEnd, employeeEmail)) {
            println(parametersValid(dateStart, dateEnd, employeeEmail))
            if (dateStart<dateEnd)
            {
                val dateStartEnd: MutableList<LocalDate> = convertParameters(dateStart, dateEnd)
                val yearsDay: MutableMap<String, Int> =
                    usedVacationDaysService.getDaysBetweenDate(dateStartEnd[0], dateStartEnd[1])
                val employee: Employee = employeeService.findEmployeeByEmail(employeeEmail)
                lateinit var vacationDayPerYear: VacationDayPerYear
                var newDay: Int = 0
                yearsDay.forEach { (k, v) ->
                    vacationDayPerYear = vacationDayPerYearService.findByYearAndEmployeeId(k, employee)
                    newDay = vacationDayPerYear.day - v
                    if (newDay > 0) {
                        vacationDayPerYearService.updateVacationDayPerYears(newDay, k, employee)
                        usedVacationService.saveUsedVacation(UsedVacation(0, dateStartEnd[0], dateStartEnd[1], employee))
                    } else {
                        return throw ResponseStatusException(
                            HttpStatus.NOT_ACCEPTABLE, MessageError.DaysOut.message
                        )
                    }
                }
            }
            else{
                throw ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, MessageError.WrongDate.message
                )
            }

        } else {
            throw ResponseStatusException(
                HttpStatus.NOT_ACCEPTABLE, MessageError.InvalidParameters.message
            )
        }
    }

    fun employeeInfo(employeeEmail: String,year:String): MutableMap<String, MutableList<Int>> {
        val emailValidated: Boolean = EmailValidator.getInstance().isValid(employeeEmail)
        val employee: Employee = employeeService.findEmployeeByEmail(employeeEmail)

        var usedVacationEmployee: MutableList<UsedVacation> = mutableListOf()

        //used days per year
        var daysPerYear: MutableMap<String, Int> = mutableMapOf()

        var daysPerYearUsedVacation: MutableMap<String, Int> = mutableMapOf()

        if (emailValidated && employee != null) {
                usedVacationEmployee = usedVacationService.datesPerEmployee(employee.id)
            usedVacationEmployee.forEach {
                daysPerYearUsedVacation = usedVacationDaysService.getDaysBetweenDate(it.dateStart, it.dateEnd)
                    daysPerYearUsedVacation.forEach { (t) ->
                        if (daysPerYear.containsKey(t)) {
                            daysPerYear.set(
                                key = t,
                                daysPerYear.getValue(key = t) + daysPerYearUsedVacation.getValue(key = t)
                            )
                        } else {
                            daysPerYear.set(key = t, value = daysPerYearUsedVacation.getValue(key = t))
                        }
                    }
            }

        }
        var daysPerYearWithListDays: MutableMap<String, MutableList<Int>> = mutableMapOf()

        daysPerYear.forEach { (t, u) ->
            if (year==t) {
                var daysLeft: Int = vacationDayPerYearService.findByYearAndEmployeeId(t, employee).day
                var daysTotalLeftUsed: MutableList<Int> = mutableListOf()
                daysTotalLeftUsed.add(u + daysLeft)
                daysTotalLeftUsed.add(daysLeft)
                daysTotalLeftUsed.add(u)
                daysPerYearWithListDays.set(key = t, value = daysTotalLeftUsed)
            }
        }

        return daysPerYearWithListDays
    }

}