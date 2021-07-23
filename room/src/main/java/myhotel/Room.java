package myhotel;

import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="Room_table")
public class Room {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Integer price;
    private Long hostId;

    @PostPersist
    public void onPostPersist(){
        RegisteredRoom registeredRoom = new RegisteredRoom();
        BeanUtils.copyProperties(this, registeredRoom);
        registeredRoom.publishAfterCommit();

    }

    @PostRemove
    public void onPostRemove(){
        DeletedRoom deletedRoom = new DeletedRoom();
        BeanUtils.copyProperties(this, deletedRoom);
        deletedRoom.publishAfterCommit();
    }

}
