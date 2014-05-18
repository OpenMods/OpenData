<?php

namespace OpenData\Controllers;

use Symfony\Component\HttpFoundation\Request;

class BrowseController {

    private $twig;
    private $mongo;

    private static $TABLES = array(
        'analytics',
        'crashes',
        'files',
        'mods',
        'urls',
        'common_crashes',
        'packages'
    );

    public function __construct($twig, $mongo) {
        $this->twig = $twig;
        $connections = $mongo;
        $conn = $connections['default'];
        $this->mongo = $conn->hopper;
    }

    public function index(Request $request) {
        return $this->twig->render('browse.twig');
    }

    public function table(Request $request, $table) {

        if (!in_array($table, self::$TABLES)) {
            throw new \Exception();
        }

        $iterator = $this->mongo->$table->find();
        $page = $request->get('page', 1);
        $perPage = 50;
        $skip = ($page - 1) * $perPage;
        $total = $iterator->count();
        $pageCount = max(1, ((int) ($total - 1) / $perPage) + 1);

        if ($page > $pageCount || $page < 1) {
            throw new \Exception('nope');
        }

        $results = $iterator->skip($skip)->limit($perPage);
        $formattedResults = array();

        foreach ($results as $result) {
            array_walk_recursive($result, function (&$item, $key) {
                if ($item instanceof \MongoDate) {
                    $item = date('c', $item->sec);
                }
            });

            $formattedResults[] = $result;
        }

        $response = array(
            'results' => $formattedResults,
            'page_count' => $pageCount,
            'current_page' => $page,
            'total' => $total,
            'disablePrev' => $page <= 1,
            'disableNext' => $page + 1 >= $pageCount,
            'table' => $table
        );
        if ($request->get('format') == 'json') {
            return new \Symfony\Component\HttpFoundation\JsonResponse($response);
        }

        return $this->twig->render('browse.twig', $response);
    }

    private function getPagination($results, $page = 1, $perPage = 20) {



    }
}
