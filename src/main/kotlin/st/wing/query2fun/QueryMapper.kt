package st.wing.query2fun

import com.google.common.base.CaseFormat
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Statement
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import st.wing.query2fun.annotations.QueryClass
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import javax.inject.Inject
import javax.inject.Named

@Named
class QueryMapper(
    @Inject
    private val connection: Connection,
) {
    private val clazz = Class.forName("st.wing.query2fun.QueryMapperStorage");
    private val instance = clazz.newInstance()
    private val pkg = getPkgName()

    private var sqlMap: Map<String, String>;
    var handMap: Map<String, (Array<Any>) -> Any>;
    var proxy: Any;

    init {
        println("--------------------")
        this.getClass().let {
            proxy = Proxy.newProxyInstance(
                Thread.currentThread().contextClassLoader,
                it.toTypedArray()
            ) { _, method, args ->
                println(Regex("<([^\\<^\\>.]+?)>").find(method.genericReturnType.typeName)?.groupValues)
                query(method, args)
            }
        }

        sqlMap = getMap()
        handMap = getHandlerMap()
    }

    private fun getMap(): Map<String, String> {
        return clazz.getDeclaredField("SQL").let {
            it.isAccessible = true
            it
        }.get(instance) as Map<String, String>
    }


    private fun getPkgName(): String {
        return clazz.getDeclaredField("PKG").let {
            it.isAccessible = true
            it
        }.get(instance) as String
    }

    private fun getHandlerMap(): Map<String, (Array<Any>) -> Any> {
        return this.getClass().flatMap { clazz ->
            clazz.declaredMethods.map { method ->
                val key = getName(method)
                Pair(key, createHandler(connection.createStatement(sqlMap[key]), method))
            }
        }.toMap()
    }

    private fun getClass(): List<Class<*>> {
        return ClassPath.getClassWithAnnotation<QueryClass>(pkg)
    }

    inline fun <reified T> get(): T {
        return proxy as T
    }


    private fun createHandler(
        statement: Statement,
        method: Method,
    ): (Array<Any>) -> Any {
        val (
            resultType,
            isResultList,
        ) = getReturnType(method)
        return ({ args ->
            args.forEachIndexed { index, value ->
                statement.bind("$${index + 1}", value)
            }
            statement.execute().toFlux().flatMap { result ->
                result.map { row, meta ->
                    println(row.get(meta.columnNames.first()))
                    println(meta.columnNames.first())
                    resultType.declaredConstructors.map {
                        println(it.parameters.size)
                        println(it.isAccessible)
                    }
                    resultType.declaredConstructors.first { it.parameterCount == meta.columnNames.size }
                        .let {
                            it.isAccessible = true
                            val parameters = meta.columnNames.map { name -> row.get(name) }.toTypedArray()
                            println(parameters)
                            it.newInstance(*parameters)
                        }
                }.toFlux()
            }.let {
                if (!isResultList) it.take(1).toMono()
                else it
            }
        })
    }

    private fun getReturnType(method: Method): Pair<Class<*>, Boolean> {
        method.genericReturnType.typeName.trimEnd('>').split("<").let {
            return Pair(Class.forName(it.last()), it.first().endsWith("Flux"))
        }
    }

    private fun getName(method: Method): String {
        method.declaringClass.name.split(".").map {
            CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, it)
        }.joinToString(".").let {
            return "${it}.${method.name.toLowerCase()}"
        }
    }

    private fun query(method: Method, args: Array<Any>): Any {
        return (handMap[getName(method)]!!)(args)
    }


}

