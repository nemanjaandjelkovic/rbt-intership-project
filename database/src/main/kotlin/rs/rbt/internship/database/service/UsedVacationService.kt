package rs.rbt.internship.database.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import rs.rbt.internship.database.model.Employee
import rs.rbt.internship.database.model.UsedVacation
import rs.rbt.internship.database.repository.UsedVacationRepository
import java.time.LocalDate

@Service
class UsedVacationService() {

    @Autowired
    lateinit var usedVacationRepository: UsedVacationRepository

    fun saveUsedVacations(usedVacation: MutableList<UsedVacation>) {
        usedVacationRepository.saveAll(usedVacation)
    }

    fun saveUsedVacation(usedVacation: UsedVacation)
    {
        usedVacationRepository.save(usedVacation)
    }

    fun dates(dateStart:LocalDate,dateEnd:LocalDate,employeeId:Long):MutableList<UsedVacation>
    {
        return usedVacationRepository.findAllByDateStartGreaterThanEqualAndDateEndLessThanEqualAndEmployeeIdEquals(dateStart,dateEnd,employeeId)
    }

    fun datesPerEmployee(employeeId: Long):MutableList<UsedVacation>
    {
        return usedVacationRepository.findAllByEmployeeIdEquals(employeeId)
    }

    fun existsUsedVacation(dateStart:LocalDate,dateEnd:LocalDate,employeeEmail:String):Boolean
    {
        return usedVacationRepository.existsByDateStartAndDateEndAndEmployeeEmail(dateStart,dateEnd,employeeEmail)
    }

    fun deleteAllUsedVacation()
    {
        usedVacationRepository.deleteAll()
    }
}