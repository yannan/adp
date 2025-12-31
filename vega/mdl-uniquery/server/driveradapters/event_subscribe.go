package driveradapters

func (s *SubHandler) Listen() {

	go func() {
		exitCh := make(chan bool)
		_ = s.subService.Subscribe(exitCh)
	}()
}
