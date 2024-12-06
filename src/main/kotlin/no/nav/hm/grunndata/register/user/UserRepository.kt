package no.nav.hm.grunndata.register.user

import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface UserRepository: CoroutineCrudRepository<User, UUID>, CoroutineJpaSpecificationExecutor<User> {

    suspend fun findByEmailIgnoreCase(email: String): User?

    @Query("""INSERT INTO $USER_V1(id, name, email, token, roles, attributes, created, updated) 
        VALUES (:id, :name, lower(:email), crypt(:token, gen_salt('bf', 8)), :roles::json, :attributes::json, :created, :updated)"""
    )
    suspend fun createUser(user: User)

    @Query(
        """SELECT * FROM $USER_V1 WHERE lower(email) = lower(:email) AND
                          token = crypt(:token, token)""")
    suspend fun loginUser(email:String, token:String): User?

    @Query("""UPDATE $USER_V1 SET token = crypt(:token,gen_salt('bf', 8)) WHERE id = :id  AND token = crypt(:oldToken, token)""")
    suspend fun changePassword(id: UUID,  oldToken: String, token: String)

    @Query("""UPDATE $USER_V1 SET token = crypt(:token,gen_salt('bf', 8)) WHERE id = :id """)
    suspend fun updatePassword(id: UUID, token: String)

    @Query("""SELECT * FROM $USER_V1 WHERE jsonb_extract_path_text(attributes,'supplierId') = :supplierId """)
    suspend fun getUsersBySupplierId(supplierId: String): List<User>

}
