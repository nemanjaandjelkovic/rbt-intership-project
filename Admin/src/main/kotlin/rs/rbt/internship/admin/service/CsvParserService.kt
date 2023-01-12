package rs.rbt.internship.admin.service

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import rs.rbt.internship.admin.dto.EmployeeResponseDto
import rs.rbt.internship.admin.dto.UsedVacationResponseDto
import rs.rbt.internship.admin.dto.VacationDayPerYearsResponseDto
import rs.rbt.internship.admin.exception.CsvColumnName
import rs.rbt.internship.admin.exception.CsvMessageError
import rs.rbt.internship.admin.exception.CustomException
import rs.rbt.internship.admin.exception.CustomResponseEntity
import rs.rbt.internship.admin.extenstion.toResponse
import rs.rbt.internship.database.model.Employee
import rs.rbt.internship.database.model.UsedVacation
import rs.rbt.internship.database.model.VacationDayPerYear
import rs.rbt.internship.database.service.EmployeeService
import rs.rbt.internship.database.service.UsedVacationService
import rs.rbt.internship.database.service.VacationDayPerYearService
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@Service
class CsvParserService {
    companion object;

    @Autowired
    lateinit var employeeServices: EmployeeService

    @Autowired
    lateinit var usedVacationDayPerYearService: VacationDayPerYearService

    var usedVacationDaysService: UsedVacationDaysService = UsedVacationDaysService()

    @Autowired
    lateinit var parametersCheckService: ParametersCheckService

    @Autowired
    lateinit var usedVacationService: UsedVacationService

    fun csvParseEmployee(file: MultipartFile): CustomResponseEntity {

        val fileReader = BufferedReader(InputStreamReader(file.inputStream, "UTF-8"))
        val csvParser = CSVParser(
            fileReader, CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreHeaderCase()
                .withIgnoreEmptyLines()
                .withTrim()
        )


        val headers: List<String> = csvParser.headerNames

        //return params
        val employeesDTO: MutableList<EmployeeResponseDto> = mutableListOf()
        val employees: MutableList<Employee> = mutableListOf()
        var message: String = CsvMessageError.ALL_VALID.message
        var httpStatus: HttpStatus = HttpStatus.OK

        val csvRecords: Iterable<CSVRecord> = csvParser.records
        val returnValue: MutableMap<HttpStatus, MutableList<Employee>> = mutableMapOf()


        csvRecords.forEach {
            if (it.recordNumber != 1L) {
                val employee: Employee = Employee(
                    email = it.get(0),
                    password = it.get(1)
                )
                if (parametersCheckService.checkEmail(it.get(0))) {
                    if (!employeeServices.employeeExists(it.get(0))) {
                        message = CsvMessageError.OK.message
                        employees.add(employee)
                    } else {
                        httpStatus = HttpStatus.PARTIAL_CONTENT
                        message = CsvMessageError.EMPLOYEE_EXISTS.message
                    }
                } else {
                    httpStatus = HttpStatus.PARTIAL_CONTENT
                    message = CsvMessageError.WRONG_EMAIL_FORMAT.message
                }
                employeesDTO.add(employee.toResponse(employee, message))
            } else {
                if (it.get(0) != CsvColumnName.EMAIL.columnName && it.get(1) != CsvColumnName.PASSWORD.columnName) {
                    throw CustomException(HttpStatus.NOT_ACCEPTABLE, CsvMessageError.WRONG_CSV.message, employeesDTO)
                }
            }
        }
        return CustomResponseEntity(httpStatus, employees, employeesDTO)
    }

    fun csvParseUsedVacation(file: MultipartFile): CustomResponseEntity {

        val fileRead = BufferedReader(InputStreamReader(file.inputStream, "UTF-8"))
        val csvParser = CSVParser(
            fileRead, CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreHeaderCase()
                .withTrim()
        )
        val usedVacations: MutableList<UsedVacation> = mutableListOf()
        val usedVacationsDTO: MutableList<UsedVacationResponseDto> = mutableListOf()
        val csvRecords: Iterable<CSVRecord> = csvParser.records
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
        var day: MutableMap<String, Int>

        //return params
        val employees: MutableList<Employee> = mutableListOf()
        var message: String = CsvMessageError.ALL_VALID.message
        var httpStatus: HttpStatus = HttpStatus.OK

        if (csvParser.headerNames.size != 3) {
            return throw ResponseStatusException(
                HttpStatus.NOT_ACCEPTABLE, CsvMessageError.WRONG_CSV.message
            )
        }
        if (csvParser.headerNames[0] != CsvColumnName.EMPLOYEE.columnName && csvParser.headerNames[1] != CsvColumnName.START_DATE.columnName && csvParser.headerNames[2] != CsvColumnName.END_DATE.columnName) {
            return throw ResponseStatusException(
                HttpStatus.NOT_ACCEPTABLE, CsvMessageError.WRONG_CSV.message
            )
        }
        csvRecords.forEach { it ->
            if (it.size() != 3) {
                return throw ResponseStatusException(
                    HttpStatus.NOT_ACCEPTABLE, CsvMessageError.WRONG_CSV_ROW.message
                )
            }
            if (LocalDate.parse(it.get(1), formatter) > LocalDate.parse(it.get(2), formatter)) {
                message = CsvMessageError.WRONG_DATE.message
                httpStatus = HttpStatus.NOT_ACCEPTABLE
            }
            lateinit var usedVacation: UsedVacation
            // if employee exists
            if (employeeServices.employeeExists(it.get(0)) && parametersCheckService.checkEmail(it.get(0))) {
                usedVacation = UsedVacation(
                    dateStart = LocalDate.parse(it.get(1), formatter),
                    dateEnd = LocalDate.parse(it.get(2), formatter),
                    employee = employeeServices.findEmployeeByEmail(it.get(0))
                )
                // if usedVacation exists (if not do =>)
                if (!usedVacationService.existsUsedVacation(
                        usedVacation.dateStart,
                        usedVacation.dateEnd,
                        usedVacation.employee.email
                    )
                ) {

                    day = usedVacationDaysService.getDaysBetweenDate(usedVacation.dateStart, usedVacation.dateEnd)
                    var yearDayLeft: VacationDayPerYear

                    day.forEach {
                        println(it)
                        try {
                            yearDayLeft =
                                usedVacationDayPerYearService.findByYearAndEmployeeId(it.key, usedVacation.employee)

                            if (yearDayLeft.day - it.value >= 0 && it.key == yearDayLeft.year) {
                                usedVacations.add(usedVacation)
                                usedVacationsDTO.add(usedVacation.toResponse(usedVacation, CsvMessageError.OK.message))
                                usedVacationDayPerYearService.updateVacationDayPerYears(
                                    it.value,
                                    it.key,
                                    usedVacation.employee
                                )
                            }
                        } catch (e: Exception) {
                            println("EXCEPTION")
                        }
                    }

                } else {
                    message = CsvMessageError.USED_VACATION_EXISTS.message
                    httpStatus = HttpStatus.PARTIAL_CONTENT
                    usedVacationsDTO.add(usedVacation.toResponse(usedVacation, message))
                }
            } else {
                message = CsvMessageError.NOT_FOUND_EMPLOYEE.message
                httpStatus = HttpStatus.NOT_FOUND
                usedVacationsDTO.add(usedVacation.toResponse(usedVacation, message))
            }
        }

        return CustomResponseEntity(httpStatus, usedVacations, usedVacationsDTO)
    }


    fun csvParseVacationDayPerYears(file: MutableList<MultipartFile>): CustomResponseEntity {

        //return params
        val vacationDayPerYears: MutableList<VacationDayPerYear> = mutableListOf()
        val vacationDayPerYearsDTO: MutableList<VacationDayPerYearsResponseDto> = mutableListOf()
        var message: String = CsvMessageError.ALL_VALID.message
        var httpStatus: HttpStatus = HttpStatus.OK

        file.forEach { it ->
            val fileRead = BufferedReader(InputStreamReader(it.inputStream, "UTF-8"))
            val csvParser = CSVParser(
                fileRead, CSVFormat.DEFAULT.withFirstRecordAsHeader()
            )
            val headers: List<String> = csvParser.headerNames
            val csvRecords: Iterable<CSVRecord> = csvParser.records
            val employee = Employee()

            lateinit var vacationDayPerYear: VacationDayPerYear

            csvRecords.forEach {
                if (it.recordNumber != 1L) {
                    //if employee exists and year have 4 char
                    if (employeeServices.employeeExists(it.get(0)) && parametersCheckService.checkEmail(it.get(0)) && headers[1].length == 4) {
                        vacationDayPerYear = VacationDayPerYear(
                            year = headers[1],
                            day = Integer.parseInt(it.get(1)),
                            employee = employeeServices.findEmployeeByEmail(it.get(0))
                        )
                        //if usedVacationDayPerYear don't exist
                        if (!usedVacationDayPerYearService.existsVacationDayPerYear(
                                vacationDayPerYear.year,
                                vacationDayPerYear.employee.id
                            )
                        ) {
                            vacationDayPerYears.add(vacationDayPerYear)
                            message = CsvMessageError.OK.message
                            vacationDayPerYearsDTO.add((vacationDayPerYear.toResponse(vacationDayPerYear, message)))
                            employeeServices.saveEmployee(employee)
                        } else {
                            message = CsvMessageError.VACATION_DAYS_PER_YEAR_EXISTS.message
                            httpStatus = HttpStatus.PARTIAL_CONTENT
                            vacationDayPerYearsDTO.add((vacationDayPerYear.toResponse(vacationDayPerYear, message)))
                        }
                    } else {
                        message = CsvMessageError.NOT_FOUND_EMPLOYEE.message
                        httpStatus = HttpStatus.PARTIAL_CONTENT
                        vacationDayPerYearsDTO.add((vacationDayPerYear.toResponse(vacationDayPerYear, message)))
                    }
                } else {
                    if (it.get(0) != CsvColumnName.EMPLOYEE.columnName && it.get(1) != CsvColumnName.TOTAL_VACATION_DAYS.name) {
                        return throw ResponseStatusException(
                            HttpStatus.NOT_ACCEPTABLE, CsvMessageError.WRONG_CSV.message
                        )
                    }
                }
            }
        }

        return CustomResponseEntity(httpStatus, vacationDayPerYears, vacationDayPerYearsDTO)
    }


}