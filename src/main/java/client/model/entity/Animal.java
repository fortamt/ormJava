package client.model.entity;

import lombok.*;
import orm.annotation.Column;
import orm.annotation.Entity;
import orm.annotation.Id;
import orm.annotation.Table;

import java.time.LocalDate;

@Entity
@Data
@Table(name = "Animal")
@NoArgsConstructor
@RequiredArgsConstructor
public class Animal {
    @Id(name = "id")
    Long id;
    @Column(name="name")
    @NonNull String name;
    @Column(name="birth_date")
    @NonNull LocalDate birthDate;

}
