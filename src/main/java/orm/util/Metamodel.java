package orm.util;

import orm.annotation.Column;
import orm.annotation.Id;
import orm.annotation.Table;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class Metamodel {

    private final Class<?> clss;
    private final Table tableName;

    public static Metamodel of(Class<?> clss) {
        return new Metamodel(clss);
    }

    public Metamodel(Class<?> clss) {
        this.clss = clss;
        this.tableName = this.clss.getAnnotation(Table.class);
    }

    public Class<?> getClassName() {
        return clss;
    }

    public List<ColumnField> getColumns() {
        List<ColumnField> columnFields = new ArrayList<>();
        Field[] fields = clss.getDeclaredFields();
        for (Field field : fields) {
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                ColumnField columnField = new ColumnField(field);
                columnFields.add(columnField);
            }
        }
        return columnFields;
    }

    public IdField getPrimaryKey() {
        Field[] fields = clss.getDeclaredFields();
        for (Field field : fields) {
            Id primaryKey = field.getAnnotation(Id.class);
            if (primaryKey != null) {
                IdField primaryKeyField = new IdField(field);
                return primaryKeyField;
            }
        }
        throw new IllegalArgumentException("No primary key found in class " + clss.getSimpleName());
    }

    public String buildInsertSqlRequest() {
        String columnElement = buildColumnNames();
        String questionMarksElement = buildQuestionMarksElement();

        return "insert into " + this.clss.getSimpleName() +
                " (" + columnElement + ") values (" + questionMarksElement + ")";
    }

    private String buildQuestionMarksElement() {
        int numberOfColumns = getColumns().size();
        String questionMarksElement =
                IntStream.range(0, numberOfColumns)
                        .mapToObj(index -> "?")
                        .collect(Collectors.joining(", "));
        return questionMarksElement;
    }

    private String buildColumnNames() {
        List<String> columnNames = getColumns()
                .stream()
                .map(ColumnField::getName)
                .collect(Collectors.toList());
        String columnElement = String.join(", ", columnNames);
        return columnElement;
    }

    public String buildTableInDbRequest() {
        String table = tableName.name();
        if(Objects.equals(table, "")){
            table = this.clss.getName();
        }
        String id = getPrimaryKey().getName();
        String columns = getColumns().stream()
                .map(el -> {
                    String result = "";
                    if(el.getName().equals("")){
                        result+= el.getField().getName() + " ";
                    } else {
                        result+= el.getName()+ " ";
                    }
                    if(el.getType() == String.class){
                        result+= "varchar(50)";
                    }
                    if(el.getType() == LocalDate.class){
                        result+= "date";
                    }
                    return result;
                })
                .collect(Collectors.joining(", "));

        return "create table if not exists " + table + " (" +
                id + " int not null auto_increment," +
                columns +
                ", primary key ("+id+")" +
                ")";
    }

    public String buildRemoveSqlRequest() {
        String sql = "delete from " + clss.getSimpleName() + " where id = ?";
        return sql;
    }
}
