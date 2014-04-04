<?php

namespace OpenData\Services;

class BaseService {

    protected $connections;
    protected $conn;
    protected $db;

    public function __construct($connections) {
        $this->connections = $connections;
        $this->conn = $connections['default'];
        $this->db = $this->conn->hopper;
    }

}
