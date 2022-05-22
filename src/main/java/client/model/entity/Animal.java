package client.model.entity;

import lombok.*;
import orm.annotation.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "Animal")
@NoArgsConstructor
@RequiredArgsConstructor
public class Animal {
    @Id(name = "id")
    Long id;
    @Column(name = "name")
    @NonNull String name;
    @Column(name = "birth_date")
    @NonNull LocalDate birthDate;

    @ManyToOne(name = "zoo_id")
    Zoo zoo;

    public void moveToZoo(Zoo newZoo) {
        zoo.removeAnimal(this);
        newZoo.addAnimal(this);
    }

    @Override
    public String toString() {
        return "Animal{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", birthDate=" + birthDate +
                ", zooName=" + zoo.getName() +
                '}';
    }

    @Override
    public int hashCode() {
        return 20;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Animal other = (Animal) obj;
        return id != null && id.equals(other.getId());
    }
}
