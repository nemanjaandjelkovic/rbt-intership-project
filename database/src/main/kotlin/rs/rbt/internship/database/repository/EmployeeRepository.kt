package rs.rbt.internship.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import rs.rbt.internship.database.model.Employee

@Repository
interface EmployeeRepository:JpaRepository<Employee,Long> {
    fun findEmployeeByEmail(email:String):Employee

    fun existsEmployeeByEmail(email:String):Boolean

    fun existsEmployeeByEmailAndPassword(email:String,password:String):Boolean




}