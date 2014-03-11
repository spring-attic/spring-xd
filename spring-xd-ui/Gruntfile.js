'use strict';

module.exports = function (grunt) {

  // Load grunt tasks automatically
  require('load-grunt-tasks')(grunt);

  grunt.loadNpmTasks('grunt-bower-task');
  grunt.loadNpmTasks('grunt-connect-proxy');

  // Time how long tasks take. Can help when optimizing build times
  require('time-grunt')(grunt);

  // Define the configuration for all the tasks
  grunt.initConfig({

    // Project settings
    xd: {
      // configurable paths
      app: 'app',
      dist: 'dist'
    },
    bower: {
      options: {
        targetDir: '<%= xd.app %>/lib'
      },
      install: {
      }
    },
    // Watches files for changes and runs tasks based on the changed files
    watch: {
      js: {
        files: ['<%= xd.app %>/scripts/{,*/}*.js'],
        tasks: ['newer:jshint:all'],
        options: {
          livereload: true
        }
      },
      jsTest: {
        files: ['test/spec/{,*/}*.js'],
        tasks: ['newer:jshint:test', 'karma']
      },
      less: {
        files: ['<%= xd.app %>/styles/{,*/}*.less'],
        tasks: ['less']
      },
      styles: {
        files: ['<%= xd.app %>/styles/{,*/}*.css'],
        tasks: ['newer:copy:styles', 'autoprefixer']
      },
      gruntfile: {
        files: ['Gruntfile.js']
      },
      livereload: {
        options: {
          livereload: '<%= connect.options.livereload %>'
        },
        files: [
          '<%= xd.app %>/**/*.html',
          '.tmp/styles/{,*/}*.css',
          '<%= xd.app %>/images/{,*/}*.{png,jpg,jpeg,gif}'
        ]
      }
    },

    // The actual grunt server settings
    connect: {
      options: {
        port: 8000,
        // Set to '0.0.0.0' to access the server from outside.
        hostname: '0.0.0.0',
        livereload: 35729
      },
      livereload: {
        options: {
          open: true,
          base: [
            '.tmp',
            '<%= xd.app %>'
          ],
          middleware: function (connect, options) {

            if (!Array.isArray(options.base)) {
              options.base = [options.base];
            }
            var middlewares = [require('grunt-connect-proxy/lib/utils').proxyRequest];

            options.base.forEach(function (base) {
              grunt.log.warn(base);
              middlewares.push(connect.static(base));
            });
            return middlewares;
          }
        }
      },
      test: {
        options: {
          port: 9001,
          base: [
            '.tmp',
            'test',
            '<%= xd.app %>'
          ]
        }
      },
      dist: {
        options: {
          base: '<%= xd.dist %>'
        }
      },
      proxies: [
        {
          context: ['/batch', '/job'],
          host: 'localhost',
          port: 9393,
          changeOrigin: true
        }
      ]
    },

    // Make sure code styles are up to par and there are no obvious mistakes
    jshint: {
      options: {
        jshintrc: '.jshintrc',
        reporter: require('jshint-stylish')
      },
      all: [
        'Gruntfile.js',
        '<%= xd.app %>/scripts/{,*/}*.js'
      ],
      test: {
        options: {
          jshintrc: 'test/.jshintrc'
        },
        src: ['test/spec/{,*/}*.js']
      }
    },
    less: {
      dist: {
        files: {
          '<%= xd.app %>/styles/main.css': ['<%= xd.app %>/styles/main.less']
        },
        options: {
          sourceMap: true,
          sourceMapFilename: '<%= xd.app %>/styles/main.css.map',
          sourceMapBasepath: '<%= xd.app %>/',
          sourceMapRootpath: '/'
        }
      }
    },
    // Empties folders to start fresh
    clean: {
      dist: {
        files: [
          {
            dot: true,
            src: [
              '.tmp',
              '<%= xd.dist %>/*',
              '!<%= xd.dist %>/.git*'
            ]
          }
        ]
      },
      server: '.tmp'
    },

    // Add vendor prefixed styles
    autoprefixer: {
      options: {
        browsers: ['last 1 version']
      },
      dist: {
        files: [
          {
            expand: true,
            cwd: '.tmp/styles/',
            src: '{,*/}*.css',
            dest: '.tmp/styles/'
          }
        ]
      }
    },
    // Automatically inject Bower components into the app
    'bower-install': {
      app: {
        src: '<%= xd.app %>/index.html',
        ignorePath: '<%= xd.app %>/'
      }
    },
    // Renames files for browser caching purposes
    rev: {
      dist: {
        files: {
          src: [
            // TODO: commenting out js files for now.
            // '<%= xd.dist %>/scripts/{,*/}*.js',
            '<%= xd.dist %>/styles/{,*/}*.css',
            '<%= xd.dist %>/images/{,*/}*.{png,jpg,jpeg,gif}',
            '<%= xd.dist %>/styles/fonts/*'
          ]
        }
      }
    },
    // Reads HTML for usemin blocks to enable smart builds that automatically
    // concat, minify and revision files. Creates configurations in memory so
    // additional tasks can operate on them
    useminPrepare: {
      html: '<%= xd.app %>/index.html',
      options: {
        dest: '<%= xd.dist %>'
      }
    },
    // Performs rewrites based on rev and the useminPrepare configuration
    usemin: {
      html: ['<%= xd.dist %>/{,*/}*.html'],
      css: ['<%= xd.dist %>/styles/{,*/}*.css'],
      options: {
        assetsDirs: ['<%= xd.dist %>', '<%= xd.dist %>/images']
      }
    },
    // The following *-min tasks produce minified files in the dist folder
    imagemin: {
      dist: {
        files: [
          {
            expand: true,
            cwd: '<%= xd.app %>/images',
            src: '{,*/}*.{png,jpg,jpeg,gif}',
            dest: '<%= xd.dist %>/images'
          }
        ]
      }
    },
    htmlmin: {
      dist: {
        options: {
          collapseWhitespace: true,
          collapseBooleanAttributes: true,
          removeCommentsFromCDATA: true,
          removeOptionalTags: true
        },
        files: [
          {
            expand: true,
            cwd: '<%= xd.dist %>',
            src: ['*.html', 'views/{,*/}*.html'],
            dest: '<%= xd.dist %>'
          }
        ]
      }
    },
    // Allow the use of non-minsafe AngularJS files. Automatically makes it
    // minsafe compatible so Uglify does not destroy the ng references
//    ngmin: {
//      dist: {
//        files: [
//          {
//            expand: true,
//            cwd: '.tmp/concat/js',
//            src: '*.js',
//            dest: '.tmp/concat/js'
//          }
//        ]
//      }
//    },
    // Copies remaining files to places other tasks can use
    copy: {
      dist: {
        files: [
          {
            expand: true,
            dot: true,
            cwd: '<%= xd.app %>',
            dest: '<%= xd.dist %>',
            src: [
              '*.{ico,png,txt}',
              '.htaccess',
              '*.html',
              'views/{,*/}*.html',
              'lib/**/*',
              'scripts/**/*',
              'images/*',
              'fonts/*'
            ]
          },
          {
            expand: true,
            cwd: '.tmp/images',
            dest: '<%= xd.dist %>/images',
            src: ['generated/*']
          }
        ]
      },
      styles: {
        expand: true,
        cwd: '<%= xd.app %>/styles',
        dest: '.tmp/styles/',
        src: '{,*/}*.css'
      },
      testfiles: {
        files: [
          { src: 'test/people.txt', dest: '/tmp/xd-tests/people.txt' }
        ]
      }
    },
    // Run some tasks in parallel to speed up the build process
    concurrent: {
      server: [
        'copy:styles'
      ],
      test: [
        'copy:styles'
      ],
      dist: [
        'less',
        'copy:styles'
        //'imagemin',
      ]
    },

    // Test settings
    karma: {
      options: {
        browsers: ['PhantomJS'],
        singleRun: true
      },
      e2e: {
        configFile: 'karma-e2e.conf.js'
      },
      unit: {
        configFile: 'karma.conf.js'
      }
    }
  });

  grunt.registerTask('serve', function (target) {
    if (target === 'dist') {
      return grunt.task.run(['build', 'connect:dist:keepalive']);
    }

    grunt.task.run([
      'clean:server',
      'bower-install',
      'less',
      'concurrent:server',
      'autoprefixer',
      'configureProxies:server',
      'connect:livereload',
      'watch'
    ]);
  });

  grunt.registerTask('server', function () {
    grunt.log.warn('The `server` task has been deprecated. Use `grunt serve` to start a server.');
    grunt.task.run(['serve']);
  });

  grunt.registerTask('test:unit', [
    'clean:server',
    'concurrent:test',
    'autoprefixer',
    'connect:test',
    'karma:unit'
  ]);

  grunt.registerTask('test:e2e', [
    'copy:testfiles',
    'clean:server',
    'concurrent:server',
    'configureProxies:server',
    'connect:livereload',
    'karma:e2e'
  ]);

  grunt.registerTask('build', [
    'clean:dist',
    'newer:jshint',
    'bower:install',
    'bower-install',
    'less',
    'useminPrepare',
    'concurrent:dist',
    'autoprefixer',
    'concat',
    'copy:dist',
    'cssmin',
    'rev',
    'usemin',
    'htmlmin',
    'test:unit'
  ]);

  grunt.registerTask('teste2e', [
    'test:e2e'
  ]);

  grunt.registerTask('default', [
    'build'
  ]);
};
