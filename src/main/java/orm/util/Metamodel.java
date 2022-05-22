package orm.util;

import orm.annotation.*;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Metamodel {

    private final Class<?> clss;
    private final String tableName;
    private final Map<Class<?>, String> types = Map.of(Long.class, "int", Integer.class, "int", String.class, "varchar(250)", LocalDate.class, "date");

    public static Metamodel of(Class<?> clss) {
        return new Metamodel(clss);
    }

    public Metamodel(Class<?> clss) {
        this.clss = clss;
        this.tableName = this.clss.getAnnotation(Table.class).name().equals("") ? clss.getName() : this.clss.getAnnotation(Table.class).name();
    }

    public Class<?> getClss() {
        return clss;
    }

    public String getClassName() {
        return tableName;
    }

    public List<ColumnField> getColumns() {
        List<ColumnField> columnFields = new ArrayList<>();
        Field[] fields = clss.getDeclaredFields();
        for (Field field : fields) {
            ColumnField columnField = new ColumnField(field);
            columnFields.add(columnField);
        }
        return columnFields.stream()
                .filter(el -> !el.getField().isAnnotationPresent(OneToMany.class) && !el.getField().isAnnotationPresent(Id.class))
                .toList();
    }

    public List<ColumnField> getColumnsNew() {
        List<ColumnField> columnFields = new ArrayList<>();
        Field[] fields = clss.getDeclaredFields();
        for (Field field : fields) {
            ColumnField columnField = new ColumnField(field);
            columnFields.add(columnField);
        }
        return columnFields.stream()
                .filter(el -> !el.getField().isAnnotationPresent(Id.class))
                .toList();
    }

    private List<ColumnField> getReferencesColumns() {
        List<ColumnField> columnFields = new ArrayList<>();
        Field[] fields = clss.getDeclaredFields();
        for (Field field : fields) {
            if(field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToOne.class)){
                ColumnField columnField = new ColumnField(field);
                columnFields.add(columnField);
            }
        }
        return columnFields;
    }


    public ColumnField getPrimaryKey() {
        Field[] fields = clss.getDeclaredFields();
        for (Field field : fields) {
            Id primaryKey = field.getAnnotation(Id.class);
            if (primaryKey != null) {
                return new ColumnField(field);
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
        long count = getColumns()
                .stream()
                .filter(el -> el.getField().getAnnotation(Id.class) == null)
                .count();
        return IntStream.range(0, (int) count)
                .mapToObj(index -> "?")
                .collect(Collectors.joining(", "));
    }

    private String buildColumnNames() {
        return getColumns()
                .stream()
                .map(el -> {
                    if (el.getField().isAnnotationPresent(ManyToOne.class)) {
                        return el.getField().getAnnotation(ManyToOne.class).name();
                    } else {
                        return el.getName();
                    }
                })
                .collect(Collectors.joining(", "));
    }

    public String buildTableInDbRequest() {
        String id = getPrimaryKey().getName();
        String columns = getColumns().stream()
                .map(el -> {
                    if (el.getField().isAnnotationPresent(ManyToOne.class)) {
                        return el.getField().getAnnotation(ManyToOne.class).name() + " " + types.get(Metamodel.of(el.getType()).getPrimaryKey().getType());
                    } else {
                        return el.getName() + " " + types.get(el.getType());
                    }
                })
                .collect(Collectors.joining(", "));

        return "create table if not exists " + tableName + " (" +
                id + " int not null auto_increment," +
                columns +
                ", primary key (" + id + ")" +
                ")";
    }

    public String buildConstraintSqlRequest() {
        return getColumns().stream()
                .filter(el -> el.getField().isAnnotationPresent(ManyToOne.class))
                .map(el ->
                        "ALTER TABLE " + this.tableName +
                                " ADD FOREIGN KEY (" + el.getField().getAnnotation(ManyToOne.class).name() + ") " +
                                "REFERENCES " + Metamodel.of(el.getType()).tableName + "(" + Metamodel.of(el.getType()).getPrimaryKey().getName() + ")")
                .collect(Collectors.joining(" "));
    }

    public String buildRemoveSqlRequest() {
        return "delete from " + clss.getSimpleName() + " where " + getPrimaryKey().getName() + " = ?";
    }

    public String buildSelectByIdSqlRequest() {
        return "select * from " + clss.getSimpleName() + " where id = ?";
    }

    public String buildSelectByIdRequest() {
        // select id, name, age from Person where id = ?
        return "select * from " + this.tableName +
                " where " + getPrimaryKey().getName() + " = ?";

    }

    public String buildSelectByForeignKey(Metamodel metamodel, Field field, Object id) {
        return "select " + metamodel.getPrimaryKey().getName() + " from " + this.tableName + " where " + field.getAnnotation(ManyToOne.class).name() + "=" + id.toString();
    }

    public String buildCountRowsRequest() {
        return "SELECT COUNT(*) FROM " + this.tableName;
    }

    public String buildMergeRequest() {
        String id = getPrimaryKey().getName();
        String columns = getColumns().stream()
                .map(el -> {
                    if (el.getField().isAnnotationPresent(ManyToOne.class)) {
                        return el.getField().getAnnotation(ManyToOne.class).name() + "=?";
                    } else {
                        return el.getName() + "=?";
                    }
                }).collect(Collectors.joining(", "));

        return "UPDATE " + tableName + " SET " + columns + " WHERE " + id + " = ?";
    }


    public List<ColumnField> getOneToManyColumns() {
        return getReferencesColumns().stream()
                .filter(el -> el.getField().isAnnotationPresent(OneToMany.class))
                .toList();
    }

    public List<ColumnField> getManyToOneColumns() {
        return getReferencesColumns().stream()
                .filter(el -> el.getField().isAnnotationPresent(ManyToOne.class))
                .toList();
    }

    public boolean isOneToManyPresent() {
        return getReferencesColumns()
                .stream().anyMatch(el -> el.getField().isAnnotationPresent(OneToMany.class));
    }

    public boolean isManyToOnePresent() {
        return getReferencesColumns()
                .stream().anyMatch(el -> el.getField().isAnnotationPresent(ManyToOne.class));
    }
}

