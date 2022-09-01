package cn.xbw.hotel.service.impl;

import cn.xbw.hotel.mapper.HotelMapper;
import cn.xbw.hotel.pojo.Hotel;
import cn.xbw.hotel.service.IHotelService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {
}
