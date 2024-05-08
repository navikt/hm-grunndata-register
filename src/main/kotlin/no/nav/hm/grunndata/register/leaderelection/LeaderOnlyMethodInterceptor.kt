package no.nav.hm.grunndata.register.leaderelection

import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory


@Singleton
class LeaderOnlyMethodInterceptor(private val leaderElection: LeaderElection): MethodInterceptor<Any,Any>{

    companion object {
        private val LOG = LoggerFactory.getLogger(LeaderOnlyMethodInterceptor::class.java)
    }

    override fun intercept(context: MethodInvocationContext<Any, Any>): Any? {
            val isLeader = leaderElection.isLeader()
            if (isLeader) {
                LOG.debug("Running method ${context.targetMethod} as leader")
                return context.proceed()
            }
            LOG.debug("Not leader, skipping method ${context.targetMethod}")
            return null
    }

}