package nl.clicqo.data

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.ClusterSerializable
import org.jooq.Binding
import org.jooq.BindingGetResultSetContext
import org.jooq.BindingGetSQLInputContext
import org.jooq.BindingGetStatementContext
import org.jooq.BindingRegisterContext
import org.jooq.BindingSQLContext
import org.jooq.BindingSetSQLOutputContext
import org.jooq.BindingSetStatementContext
import org.jooq.Converter
import org.jooq.JSONB
import org.jooq.conf.ParamType
import org.jooq.impl.DSL
import java.sql.SQLException
import java.sql.SQLFeatureNotSupportedException
import java.sql.Types
import java.util.Objects

class JooqJsonbObjectBinding : Binding<JSONB, ClusterSerializable> {
  override fun converter(): Converter<JSONB, ClusterSerializable> {
    return object : Converter<JSONB, ClusterSerializable> {
      override fun from(t: JSONB?): ClusterSerializable? {
        if (t == null) {
          return null
        }

        if (t.toString().startsWith("[")) {
          return JsonArray(t.toString())
        }

        return JsonObject(t.toString())
      }

      override fun to(u: ClusterSerializable?): JSONB? = if (u == null) null else JSONB.valueOf(u.toString())

      override fun fromType(): Class<JSONB> = JSONB::class.java

      override fun toType(): Class<ClusterSerializable> = ClusterSerializable::class.java
    }
  }

  @Throws(SQLException::class)
  override fun sql(ctx: BindingSQLContext<ClusterSerializable>) {
    // Depending on how you generate your SQL, you may need to explicitly distinguish
    // between jOOQ generating bind variables or inlined literals.
    if (ctx.render().paramType() == ParamType.INLINED) {
      ctx
        .render()
        .visit(DSL.inline(ctx.convert(converter()).value()))
        .sql("::jsonb")
    } else {
      ctx.render().sql(ctx.variable())
    }
  }

  @Throws(SQLException::class)
  override fun register(ctx: BindingRegisterContext<ClusterSerializable>) {
    ctx.statement().registerOutParameter(ctx.index(), Types.VARCHAR)
  }

  // Converting the JsonElement to a String value and setting that on a JDBC PreparedStatement
  @Throws(SQLException::class)
  override fun set(ctx: BindingSetStatementContext<ClusterSerializable>) {
    ctx.statement().setString(ctx.index(), Objects.toString(ctx.convert(converter()).value(), null))
  }

  // Getting a String value from a JDBC ResultSet and converting that to a ClusterSerializable
  @Throws(SQLException::class)
  override fun get(ctx: BindingGetResultSetContext<ClusterSerializable>) {
    ctx.convert(converter()).value(JSONB.valueOf(ctx.resultSet().getString(ctx.index())))
  }

  // Getting a String value from a JDBC CallableStatement and converting that to a ClusterSerializable
  @Throws(SQLException::class)
  override fun get(ctx: BindingGetStatementContext<ClusterSerializable>) {
    ctx.convert(converter()).value(JSONB.valueOf(ctx.statement().getString(ctx.index())))
  }

  // Setting a value on a JDBC SQLOutput (useful for Oracle OBJECT types)
  @Throws(SQLException::class)
  override fun set(ctx: BindingSetSQLOutputContext<ClusterSerializable>): Unit = throw SQLFeatureNotSupportedException()

  // Getting a value from a JDBC SQLInput (useful for Oracle OBJECT types)
  @Throws(SQLException::class)
  override fun get(ctx: BindingGetSQLInputContext<ClusterSerializable>): Unit = throw SQLFeatureNotSupportedException()
}
