package orm;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import orm.annotation.Id;
import orm.annotation.Column;

public class Metamodel {
    private Class<?> clss;

    public static Metamodel of(Class<?> clss) {
        return new Metamodel(clss);
    }

    public Metamodel(Class<?> clss) {
        this.clss = clss;
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

    public String buildInsertRequest() {
        // insert into Person (id, name, age) values (?, ?, ?)

        String columnElement = buildColumnNames();
        String questionMarksElement = buildQuestionMarksElement();

        return "insert into " + this.clss.getSimpleName() +
                " (" + columnElement + ") values (" + questionMarksElement + ")";
    }

    public String buildSelectRequest() {
        // select id, name, age from Person where id = ?
        String columnElement = buildColumnNames();
        return "select " + columnElement + " from " + this.clss.getSimpleName() +
                " where " + getPrimaryKey().getName() + " = ?";
    }

    private String buildQuestionMarksElement() {
        int numberOfColumns = getColumns().size() + 1;
        String questionMarksElement =
                IntStream.range(0, numberOfColumns)
                        .mapToObj(index -> "?")
                        .collect(Collectors.joining(", "));
        return questionMarksElement;
    }

    private String buildColumnNames() {
        String primaryKeyColumnName = getPrimaryKey().getName();
        List<String> columnNames =
                getColumns().stream().map(ColumnField::getName).collect(Collectors.toList());
        columnNames.add(0, primaryKeyColumnName);
        String columnElement = String.join(", ", columnNames);
        return columnElement;
    }
}
