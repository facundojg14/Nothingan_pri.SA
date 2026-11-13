package cat.copernic.Projecte1;

import cat.copernic.Projecte1.entity.Enum.Categoria;
import cat.copernic.Projecte1.entity.Enum.EstatReserva;
import cat.copernic.Projecte1.entity.Enum.EstatVehicle;
import cat.copernic.Projecte1.entity.Enum.rol;
import static cat.copernic.Projecte1.entity.Enum.rol.ADMIN;
import static cat.copernic.Projecte1.entity.Enum.rol.AGENT;
import static cat.copernic.Projecte1.entity.Enum.rol.CLIENT;
import cat.copernic.Projecte1.entity.Incidencies;
import cat.copernic.Projecte1.entity.Reserva;
import cat.copernic.Projecte1.entity.Usuaris;
import cat.copernic.Projecte1.entity.Vehicle;
import cat.copernic.Projecte1.repository.IncidenciesRepository;
import cat.copernic.Projecte1.repository.ReservaRepository;
import cat.copernic.Projecte1.repository.UsuarisRepository;
import cat.copernic.Projecte1.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class Projecte1Equip4Application {

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public static void main(String[] args) {
        SpringApplication.run(Projecte1Equip4Application.class, args);
    }

    @Bean
    public CommandLineRunner initData(
            UsuarisRepository usuarisRepo,
            VehicleRepository vehicleRepo,
            ReservaRepository reservaRepo,
            IncidenciesRepository incidenciesRepo) {
        return args -> {

            // 1. Creació d'usuaris (1 Admin + 3 Agents + 3 Clients diferents)
            crearUsuariSiNoExisteix(usuarisRepo, "Admin Principal", "alorente@alumnat.copernic.cat", "1212", "384639013", "VALIDAT", "74827465H", false, ADMIN);
            crearUsuariSiNoExisteix(usuarisRepo, "Agent 1", "fjaeggi@alumnat.copernic.cat", "1212", "456789032", "VALIDAT", "74864465N", false, AGENT);
            crearUsuariSiNoExisteix(usuarisRepo, "Agent 2", "dnavio@alumnat.copernic.cat", "1212", "234567890", "VALIDAT", "74827665T", false, AGENT);
            crearUsuariSiNoExisteix(usuarisRepo, "Agent 3", "mruiz@alumnat.copernic.cat", "1212", "678901234", "VALIDAT", "55443322K", false, AGENT);

            crearUsuariSiNoExisteix(usuarisRepo, "Carolina Macià", "cemacia@alumnat.copernic.cat", "1212", "600112233", "VALIDAT", "99887766A", true, CLIENT);
            crearUsuariSiNoExisteix(usuarisRepo, "Marc Gómez", "mgomez@alumnat.copernic.cat", "1212", "611223344", "VALIDAT", "88776655B", true, CLIENT);
            crearUsuariSiNoExisteix(usuarisRepo, "Laura Vila", "lvila@alumnat.copernic.cat", "1212", "622334455", "VALIDAT", "77665544C", true, CLIENT);

            // 2. Creació de vehicles
            crearVehicleSiNoExisteix(vehicleRepo, "3343 DGY", "Seat", "Altea", 2005, Categoria.Monovolum,new BigDecimal("24.56"), new BigDecimal("1.02"), new BigDecimal("150.00"),
                    EstatVehicle.Disponible, "vehicles/SeatAltea.jpg");

            crearVehicleSiNoExisteix(vehicleRepo, "1123 FRG", "Seat", "Leon", 2014, Categoria.Compacte, new BigDecimal("31.43"), new BigDecimal("1.31"), new BigDecimal("200.00"),
                    EstatVehicle.En_manteniment, "vehicles/SeatLeon.jpg");

            crearVehicleSiNoExisteix(vehicleRepo, "4312 NGR", "Nissan", "Micra", 2025, Categoria.Utilitari, new BigDecimal("34.76"), new BigDecimal("1.45"), new BigDecimal("200.00"),
                    EstatVehicle.Disponible, "vehicles/NissanMicra.jpg");

            crearVehicleSiNoExisteix(vehicleRepo, "8162 GWX", "Mazda", "2", 2010, Categoria.Compacte, new BigDecimal("35.12"), new BigDecimal("1.46"), new BigDecimal("180.00"),
                    EstatVehicle.Disponible, "vehicles/Mazda2.avif");

            crearVehicleSiNoExisteix(vehicleRepo, "4365 FDE", "Mercedes", "Vito", 2006, Categoria.Furgoneta, new BigDecimal("40.65"), new BigDecimal("1.69"), new BigDecimal("300.00"),
                    EstatVehicle.En_manteniment, "vehicles/MercedesVito.jpg");

            crearVehicleSiNoExisteix(vehicleRepo, "5821 LKM", "Volkswagen", "Touran", 2010, Categoria.Monovolum, new BigDecimal("35.00"), new BigDecimal("1.46"), new BigDecimal("220.00"),
                    EstatVehicle.Disponible, "vehicles/VolkswagenTouran.webp");

            // 3. Creació de reserves
            usuarisRepo.findByEmail("cemacia@alumnat.copernic.cat").ifPresent(client1 -> {
                usuarisRepo.findByEmail("mgomez@alumnat.copernic.cat").ifPresent(client2 -> {
                    usuarisRepo.findByEmail("lvila@alumnat.copernic.cat").ifPresent(client3 -> {

                        vehicleRepo.findByMatriculaIgnoreCase("3343 DGY").stream().findFirst().ifPresent(v1 -> {
                            crearReservaSiNoExisteix(reservaRepo, "RES-001", client1, v1,
                                    LocalDateTime.of(2026, 11, 10, 10, 0), LocalDateTime.of(2026, 11, 15, 10, 0),
                                    new BigDecimal("122.80"), EstatReserva.Confirmada);
                            crearReservaSiNoExisteix(reservaRepo, "RES-002", client2, v1,
                                    LocalDateTime.of(2026, 12, 1, 9, 0), LocalDateTime.of(2026, 12, 5, 18, 0),
                                    new BigDecimal("98.24"), EstatReserva.Confirmada);
                        });

                        vehicleRepo.findByMatriculaIgnoreCase("1123 FRG").stream().findFirst().ifPresent(v2 -> {
                            crearReservaSiNoExisteix(reservaRepo, "RES-003", client2, v2,
                                    LocalDateTime.of(2026, 10, 5, 10, 0), LocalDateTime.of(2026, 10, 8, 10, 0),
                                    new BigDecimal("94.29"), EstatReserva.Lliurada);
                            crearReservaSiNoExisteix(reservaRepo, "RES-004", client3, v2,
                                    LocalDateTime.of(2026, 12, 10, 10, 0), LocalDateTime.of(2026, 12, 15, 10, 0),
                                    new BigDecimal("157.15"), EstatReserva.Anulada);
                        });

                        vehicleRepo.findByMatriculaIgnoreCase("4312 NGR").stream().findFirst().ifPresent(v3 -> {
                            crearReservaSiNoExisteix(reservaRepo, "RES-005", client3, v3,
                                    LocalDateTime.of(2026, 11, 20, 9, 0), LocalDateTime.of(2026, 11, 25, 18, 0),
                                    new BigDecimal("173.80"), EstatReserva.Confirmada);
                            crearReservaSiNoExisteix(reservaRepo, "RES-006", client1, v3,
                                    LocalDateTime.of(2026, 12, 15, 10, 0), LocalDateTime.of(2026, 12, 20, 10, 0),
                                    new BigDecimal("173.80"), EstatReserva.Confirmada);
                        });

                        vehicleRepo.findByMatriculaIgnoreCase("8162 GWX").stream().findFirst().ifPresent(v4 -> {
                            crearReservaSiNoExisteix(reservaRepo, "RES-007", client1, v4,
                                    LocalDateTime.of(2026, 9, 10, 10, 0), LocalDateTime.of(2026, 9, 12, 10, 0),
                                    new BigDecimal("70.24"), EstatReserva.Lliurada);
                            crearReservaSiNoExisteix(reservaRepo, "RES-008", client2, v4,
                                    LocalDateTime.of(2026, 11, 28, 9, 0), LocalDateTime.of(2026, 12, 2, 18, 0),
                                    new BigDecimal("140.48"), EstatReserva.Confirmada);
                        });

                        vehicleRepo.findByMatriculaIgnoreCase("4365 FDE").stream().findFirst().ifPresent(v5 -> {
                            crearReservaSiNoExisteix(reservaRepo, "RES-009", client2, v5,
                                    LocalDateTime.of(2026, 10, 15, 10, 0), LocalDateTime.of(2026, 10, 18, 10, 0),
                                    new BigDecimal("121.95"), EstatReserva.Lliurada);
                            crearReservaSiNoExisteix(reservaRepo, "RES-010", client3, v5,
                                    LocalDateTime.of(2026, 12, 20, 9, 0), LocalDateTime.of(2026, 12, 30, 18, 0),
                                    new BigDecimal("406.50"), EstatReserva.Anulada);
                        });

                        vehicleRepo.findByMatriculaIgnoreCase("5821 LKM").stream().findFirst().ifPresent(v6 -> {
                            crearReservaSiNoExisteix(reservaRepo, "RES-011", client3, v6,
                                    LocalDateTime.of(2026, 9, 12, 10, 0), LocalDateTime.of(2026, 9, 16, 10, 0),
                                    new BigDecimal("140.00"), EstatReserva.Lliurada);
                            crearReservaSiNoExisteix(reservaRepo, "RES-012", client1, v6,
                                    LocalDateTime.of(2026, 12, 5, 9, 0), LocalDateTime.of(2026, 12, 10, 18, 0),
                                    new BigDecimal("175.00"), EstatReserva.Confirmada);
                        });

                            // Creació de reserva retornada amb incidència per a cemacia@alumnat.copernic.cat
                            vehicleRepo.findByMatriculaIgnoreCase("3343 DGY").stream().findFirst().ifPresent(v1 -> {
                                crearReservaSiNoExisteix(reservaRepo, "RES-013", client1, v1,
                                        LocalDateTime.of(2026, 8, 10, 10, 0), LocalDateTime.of(2026, 8, 13, 10, 0),
                                        new BigDecimal("73.68"), EstatReserva.Retornada);

                                reservaRepo.findByCodiReserva("RES-013").ifPresent(reservaRetornada -> {
                                    if (incidenciesRepo.findByReserva_Id(reservaRetornada.getId()).isEmpty()) {
                                        Incidencies inc = new Incidencies();
                                        inc.setData_Hora(LocalDate.of(2026, 8, 13));
                                        inc.setQuilometratge(45200);
                                        inc.setDescripcio("Rascada lleu al lateral posterior dret durant la devolució del vehicle.");
                                        inc.setResolucio("Pendent de peritatge i valoració al taller.");
                                        inc.setImporte(85.50);
                                        inc.setReserva(reservaRetornada);

                                        incidenciesRepo.save(inc);
                                        System.out.println("Incidència creada per a la reserva: RES-013");
                                    }
                                });
                            });
                    });
                });
            });
            
            

            System.out.println("\n----------------------------------------------------------");
            System.out.println(" Aplicació iniciada correctament amb dades de prova!");
            System.out.println("Client de prova Email :cemacia@alumnat.copernic.cat  password: 1212");
            System.out.println("Agent de prova Email :fjaeggi@alumnat.copernic.cat  password: 1212");
            System.out.println("Admin de prova Email :alorente@alumnat.copernic.cat: 1212 ");
            System.out.println("");
            System.out.println(" Accés al login: https://localhost:8443");
            System.out.println("----------------------------------------------------------\n");
        };
    }

    private void crearUsuariSiNoExisteix(UsuarisRepository repo, String nom, String email, String password, String telefon, String estat, String dni, boolean premium, rol rol) {
        if (repo.findByEmail(email).isEmpty() && repo.findByDni(dni).isEmpty()) {
            Usuaris u = new Usuaris();
            u.setNom(nom);
            u.setEmail(email);
            // CORREGUT: Encriptem la contrasenya amb BCrypt abans de guardar
            u.setPassword(passwordEncoder.encode(password));
            u.setTelefon(telefon);
            u.setEstatRegistre(estat);
            u.setDni(dni);
            u.setPremium(premium);
            u.setRol(rol);
            repo.save(u);
            System.out.println("Usuari creat: " + email + " [Rol: " + rol + "]");
        } else {
            System.out.println("L'usuari ja existeix a la BD: " + email);
        }
    }

    private void crearVehicleSiNoExisteix(VehicleRepository repo, String matricula,
            String marca, String model, int anyFabricacio, Categoria categoria,
            BigDecimal preuDia, BigDecimal preuHora, BigDecimal fiancaEstandar,
            EstatVehicle estatVehicle, String fotoVehicle) {
        if (repo.findByMatriculaIgnoreCase(matricula).isEmpty()) {
            Vehicle v = new Vehicle();
            v.setMatricula(matricula);
            v.setMarca(marca);
            v.setModel(model);
            v.setAnyFabricacio(anyFabricacio);
            v.setCategoria(categoria);
            v.setPreuDia(preuDia);
            v.setPreuHora(preuHora);
            v.setFiancaEstandar(fiancaEstandar);
            v.setEstatVehicle(estatVehicle);
            v.setFotoVehicle(fotoVehicle);
            repo.save(v);
            System.out.println("Vehicle creat: " + matricula + " [Marca: " + marca + "]");
        } else {
            System.out.println("El vehicle ja existeix a la BD: " + matricula);
        }
    }

    private void crearReservaSiNoExisteix(ReservaRepository repo, String codiReserva, Usuaris usuari, Vehicle vehicle, LocalDateTime dataInici, LocalDateTime dataFi, BigDecimal preuTotal, EstatReserva estat) {
        if (repo.findByCodiReserva(codiReserva).isEmpty()) {
            Reserva r = new Reserva();
            r.setCodiReserva(codiReserva);
            r.setUsuari(usuari);
            r.setVehicle(vehicle);
            r.setDataInici(dataInici);
            r.setDataFi(dataFi);
            r.setDataReserva(LocalDateTime.now());
            r.setPreuLloguer(preuTotal);
            r.setImportFianca(new BigDecimal("200.00"));
            r.setEstat(estat);
            repo.save(r);
            System.out.println("Reserva creada: " + codiReserva + " per a " + usuari.getNom());
        } else {
            System.out.println("La reserva " + codiReserva + " ja existeix a la BD.");
        }
    }
}