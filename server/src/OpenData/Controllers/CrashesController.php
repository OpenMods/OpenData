<?php

namespace OpenData\Controllers;

use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;

class CrashesController {

    protected $service;

    public function __construct($service) {
        $this->service = $service;
    }

    public function listAll(Request $request) {

        return new JsonResponse($this->service->getAll(
                        $request->get('skip', 0), $request->get('limit', 30)
        ));
    }

    public function save(Request $request) {
        $crash = json_decode($request->getContent(), true);
        return new JsonResponse(array("id" => $this->service->save($crash)));
    }

}
