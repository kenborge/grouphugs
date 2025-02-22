package no.kvikshaug.gh.util

import java.sql._

case class Row(name: String, rowType: String) {
  // override equals and hashCode to not be case sensitive
  override def equals(that: Any) = that match {
    case row: Row => name.equalsIgnoreCase(row.name) && rowType.equalsIgnoreCase(row.rowType)
    case _ => false
  }
  override def hashCode = name.toUpperCase.hashCode + rowType.toUpperCase.hashCode
}

case class Table(name: String, rows: List[Row]) {
  // override equals and hashCode to not be case sensitive
  override def equals(that: Any) = that match {
    case table: Table => name.equalsIgnoreCase(table.name) && rows == table.rows
    case _ => false
  }
  override def hashCode = name.toUpperCase.hashCode + rows.hashCode
}

object SQLSchema {
  private implicit def mkIterator(tableRow: ResultSet) = new Iterator[ResultSet] {
    def hasNext = tableRow.next
    def next = tableRow
  }

  def compare(dbFile: String, schemaFile: String, jdbcUrl: String): Boolean = {
    // compare as sets, so that order does not matter (note that this will not detect duplicates!)
    return getDbTables(jdbcUrl).toSet == getSchemaTables(schemaFile).toSet
  }

  private def getSchemaTables(schemaFile: String) = {
    val content = io.Source.fromFile(schemaFile).mkString
    """(?i)CREATE TABLE\s*?(\S+)\s*?\(\s*?(.*?)\s*?\)\s*?;\s*?""".r.findAllIn(content).matchData.map { m =>
      val rows = m.group(2).split(",").map(_.trim).map { column =>
        // remove all "PRIMARY KEY" entries, because we're unable to know about that from JDBC
        Row(column.substring(0, column.indexOf(' ')), column.substring(column.indexOf(' ') + 1).replaceAll("(?i)primary key", "").trim)
      }.toList
      Table(m.group(1), rows)
    }.toList
  }

  private def getDbTables(jdbcUrl: String) = {
    Class.forName("org.sqlite.JDBC")
    val connection = DriverManager.getConnection(jdbcUrl)
    val dbmd = connection.getMetaData
    val tablerows = dbmd.getTables(null, null, null, null) // lol, java and nulls, so funny
    tablerows.map { table =>
      val rows = dbmd.getColumns(null, null, table.getObject("TABLE_NAME").toString, null).map { row =>
        Row(row.getObject("COLUMN_NAME").toString, row.getObject("TYPE_NAME").toString)
      }.toList
      Table(table.getObject("TABLE_NAME").toString, rows)
    }.toList
  }
}
