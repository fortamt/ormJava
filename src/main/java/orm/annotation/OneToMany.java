package orm.annotation;

public @interface OneToMany {
    String mappedBy() default "";
}
