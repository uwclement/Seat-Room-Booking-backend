package com.auca.library.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auca.library.model.Room;
import com.auca.library.model.User;
import com.auca.library.model.UserFavoriteRoom;

@Repository
public interface UserFavoriteRoomRepository extends JpaRepository<UserFavoriteRoom, Long> {
    
    List<UserFavoriteRoom> findByUserOrderByCreatedAtDesc(User user);
    
    Optional<UserFavoriteRoom> findByUserAndRoom(User user, Room room);
    
    boolean existsByUserAndRoom(User user, Room room);
    
    @Query("SELECT ufr.room FROM UserFavoriteRoom ufr WHERE ufr.user = :user ORDER BY ufr.createdAt DESC")
    List<Room> findFavoriteRoomsByUser(@Param("user") User user);
    
    @Query("SELECT r, COUNT(ufr) as favoriteCount FROM UserFavoriteRoom ufr " +
           "JOIN ufr.room r GROUP BY r ORDER BY favoriteCount DESC")
    List<Object[]> findMostFavoritedRooms();
}
