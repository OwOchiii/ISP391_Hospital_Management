// src/main/java/orochi/service/impl/RoomServiceImpl.java
package orochi.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import orochi.model.Room;
import orochi.repository.RoomRepository;
import orochi.service.RoomService;

import java.util.List;
import java.util.Optional;

@Service
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    @Autowired
    public RoomServiceImpl(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Override
    public Optional<Room> findById(Integer roomId) {
        return roomRepository.findById(roomId);
    }

    @Override
    public Room save(Room room) {
        return roomRepository.save(room);
    }

    @Override
    public void deleteById(Integer roomId) {
        roomRepository.deleteById(roomId);
    }

    @Override
    public Integer getTotalRooms() {
        try {
            return Math.toIntExact(roomRepository.count());
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public Integer getAvailableRooms() {
        try {
            return roomRepository.countByStatus("Available");
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public List<Room> searchRooms(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return roomRepository.findAll();
        }
        return roomRepository.searchByNumberOrName(keyword.trim());
    }
}
